package ru.sterkhov.onlinepaymentsystem.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sterkhov.onlinepaymentsystem.config.DonationProperties;
import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;
import ru.sterkhov.onlinepaymentsystem.dto.payment.CreatePaymentRequest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Провайдер оплаты через DonatePay.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class DonatePayProvider implements PaymentProvider {

    /** Настройки провайдеров донатов. */
    private final DonationProperties props;

    /** HTTP-клиент для запросов к DonatePay API. */
    private final RestClient httpClient = RestClient.builder()
            .defaultHeader("Accept", "application/json")
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.DONATE_PAY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return "donatepay";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        var dp = props.getDonatePay();
        return dp.isEnabled() && dp.getPageName() != null && !dp.getPageName().isBlank();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment initiate(Payment payment, CreatePaymentRequest request) {
        var dp = props.getDonatePay();
        var refCode = "PB-" + payment.getId().toString().substring(0, 8).toUpperCase();
        var donateUrl = "https://donatepay.ru/don/" + dp.getPageName()
                + "?sum=" + payment.getAmount().toPlainString()
                + "&comment=" + refCode;

        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderName(getProviderName());
        payment.setExternalId(refCode);
        payment.setPaymentUrl(donateUrl);

        var meta = new LinkedHashMap<String, Object>();
        meta.put("refCode", refCode);
        meta.put("donateUrl", donateUrl);
        meta.put("serviceName", "DonatePay");
        payment.setMetadata(meta);

        log.info("DonatePay payment created: refCode={}, url={}", refCode, donateUrl);
        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PaymentStatus checkStatus(Payment payment) {
        var dp = props.getDonatePay();
        if (dp.getApiKey() == null || dp.getApiKey().isBlank()) {
            return PaymentStatus.PENDING;
        }

        var refCode = payment.getExternalId();
        try {
            var response = httpClient.get()
                    .uri("https://donatepay.ru/api/v1/transactions?access_token={token}&type=donation&status=success&limit=25&order=DESC",
                            dp.getApiKey())
                    .retrieve()
                    .body(Map.class);

            if (response == null) return PaymentStatus.PENDING;

            var data = (List<Map<String, Object>>) response.get("data");
            if (data == null) return PaymentStatus.PENDING;

            for (var tx : data) {
                var comment = String.valueOf(tx.get("comment"));
                if (comment.contains(refCode)) {
                    var sum = new BigDecimal(String.valueOf(tx.get("sum")));
                    if (sum.compareTo(payment.getAmount()) >= 0) {
                        log.info("DonatePay payment matched: refCode={}, sum={}", refCode, sum);
                        return PaymentStatus.PAID;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check DonatePay: {}", e.getMessage());
        }
        return PaymentStatus.PENDING;
    }
}
