package ru.sterkhov.onlinepaymentsystem.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.sterkhov.onlinepaymentsystem.config.CryptoProperties;
import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.domain.enums.CryptoNetwork;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;
import ru.sterkhov.onlinepaymentsystem.dto.payment.CreatePaymentRequest;
import ru.sterkhov.onlinepaymentsystem.exception.PaymentException;
import ru.sterkhov.onlinepaymentsystem.service.ExchangeRateService;
import ru.sterkhov.onlinepaymentsystem.service.provider.blockchain.BlockchainService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Провайдер оплаты криптовалютой через прямые блокчейн-транзакции.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class CryptoProvider implements PaymentProvider {

    /** Настройки криптовалютного провайдера. */
    private final CryptoProperties cryptoProperties;

    /** Сервис проверки транзакций в блокчейне. */
    private final BlockchainService blockchainService;

    /** Сервис курсов валют. */
    private final ExchangeRateService exchangeRateService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CRYPTO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return "crypto-direct";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return cryptoProperties.isEnabled() && cryptoProperties.getNetworks().values().stream()
                .anyMatch(CryptoProperties.NetworkProperties::isEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment initiate(Payment payment, CreatePaymentRequest request) {
        var network = resolveNetwork(request.getCryptoNetwork());
        var netProps = getNetworkProps(network);

        if (!netProps.isEnabled() || netProps.getAddress() == null || netProps.getAddress().isBlank()) {
            throw new PaymentException("Crypto network " + network + " is not configured");
        }

        var baseAmount = payment.getAmount();
        var originalCurrency = payment.getCurrency();

        if ("RUB".equalsIgnoreCase(originalCurrency)) {
            var converted = exchangeRateService.convert(baseAmount, network);
            if (converted == null) {
                throw new PaymentException("Cannot get exchange rate for " + network.getDisplayName());
            }
            baseAmount = converted;
        }

        var uniqueAmount = generateUniqueAmount(baseAmount, network);

        payment.setCurrency(network.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderName(getProviderName());
        payment.setExternalId(network.name());
        payment.setExpiresAt(Instant.now().plus(Duration.ofMinutes(cryptoProperties.getPaymentTtlMinutes())));

        var exchangeRate = exchangeRateService.getRate(network);
        var meta = new LinkedHashMap<String, Object>();
        meta.put("network", network.name());
        meta.put("networkDisplay", network.getDisplayName());
        meta.put("address", netProps.getAddress());
        meta.put("originalAmount", payment.getAmount().toPlainString());
        meta.put("originalCurrency", originalCurrency);
        if (exchangeRate != null) {
            meta.put("exchangeRate", exchangeRate.toPlainString());
        }
        meta.put("cryptoAmount", uniqueAmount.toPlainString());
        payment.setMetadata(meta);

        payment.setAmount(uniqueAmount);

        log.info("Crypto payment created: network={}, address={}, amount={} {}",
                network, netProps.getAddress(), uniqueAmount, network.getCurrency());
        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentStatus checkStatus(Payment payment) {
        if (payment.getMetadata() == null) return PaymentStatus.PENDING;

        var networkStr = (String) payment.getMetadata().get("network");
        var address = (String) payment.getMetadata().get("address");
        var cryptoAmountStr = (String) payment.getMetadata().get("cryptoAmount");

        if (networkStr == null || address == null || cryptoAmountStr == null) {
            return PaymentStatus.PENDING;
        }

        if (payment.getExpiresAt() != null && Instant.now().isAfter(payment.getExpiresAt())) {
            return PaymentStatus.EXPIRED;
        }

        var network = CryptoNetwork.valueOf(networkStr);
        var expectedAmount = new BigDecimal(cryptoAmountStr);

        var received = blockchainService.isPaymentReceived(
                network, address, expectedAmount, payment.getCreatedAt());

        if (received) {
            log.info("Crypto payment confirmed: {} {} to {}", expectedAmount, network.getCurrency(), address);
            return PaymentStatus.PAID;
        }

        return PaymentStatus.PENDING;
    }

    private CryptoNetwork resolveNetwork(String networkName) {
        if (networkName == null || networkName.isBlank()) {
            return cryptoProperties.getNetworks().entrySet().stream()
                    .filter(e -> e.getValue().isEnabled())
                    .map(e -> mapConfigKeyToNetwork(e.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new PaymentException("No crypto networks configured"));
        }
        try {
            return CryptoNetwork.valueOf(networkName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentException("Unknown crypto network: " + networkName);
        }
    }

    private CryptoProperties.NetworkProperties getNetworkProps(CryptoNetwork network) {
        var configKey = switch (network) {
            case BTC -> "btc";
            case ETH -> "eth";
            case SOL -> "sol";
            case TON -> "ton";
            case USDT_TON -> "usdt-ton";
            case USDT_TRC20 -> "usdt-trc20";
            case XAUT -> "xaut";
        };
        var props = cryptoProperties.getNetworks().get(configKey);
        if (props == null) {
            throw new PaymentException("Network " + network + " is not configured");
        }
        return props;
    }

    private CryptoNetwork mapConfigKeyToNetwork(String key) {
        return switch (key) {
            case "btc" -> CryptoNetwork.BTC;
            case "eth" -> CryptoNetwork.ETH;
            case "sol" -> CryptoNetwork.SOL;
            case "usdt-ton" -> CryptoNetwork.USDT_TON;
            case "ton" -> CryptoNetwork.TON;
            case "usdt-trc20" -> CryptoNetwork.USDT_TRC20;
            case "xaut" -> CryptoNetwork.XAUT;
            default -> throw new PaymentException("Unknown network config key: " + key);
        };
    }

    private BigDecimal generateUniqueAmount(BigDecimal baseAmount, CryptoNetwork network) {
        var suffixDigits = Math.min(network.getDecimals(), 6);
        var suffix = (System.nanoTime() % 9000) + 1000;
        var micro = BigDecimal.valueOf(suffix).movePointLeft(suffixDigits);
        return baseAmount.add(micro).setScale(suffixDigits, RoundingMode.HALF_UP);
    }

    /**
     * Возвращает карту включённых криптовалютных сетей.
     *
     * @return карта имя_сети-отображаемое_имя
     */
    public Map<String, String> getEnabledNetworks() {
        var result = new LinkedHashMap<String, String>();
        for (var entry : cryptoProperties.getNetworks().entrySet()) {
            if (entry.getValue().isEnabled()) {
                var network = mapConfigKeyToNetwork(entry.getKey());
                result.put(network.name(), network.getDisplayName());
            }
        }
        return result;
    }
}
