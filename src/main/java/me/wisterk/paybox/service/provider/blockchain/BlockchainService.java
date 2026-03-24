package me.wisterk.paybox.service.provider.blockchain;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import me.wisterk.paybox.config.CryptoProperties;
import me.wisterk.paybox.domain.enums.CryptoNetwork;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Сервис проверки транзакций в различных блокчейнах.
 */
@Service
@RequiredArgsConstructor
@ToString
@Slf4j
public class BlockchainService {

    /** Настройки криптовалютного провайдера. */
    private final CryptoProperties cryptoProperties;

    /** HTTP-клиент для запросов к блокчейн-API. */
    private final RestClient blockchainRestClient = RestClient.builder()
            .defaultHeader("Accept", "application/json")
            .build();

    /**
     * Проверяет, получен ли платёж в указанной сети.
     *
     * @param network        криптовалютная сеть
     * @param address        адрес кошелька
     * @param expectedAmount ожидаемая сумма
     * @param since          время, после которого искать транзакцию
     * @return {@code true}, если платёж подтверждён
     */
    public boolean isPaymentReceived(
            CryptoNetwork network,
            String address,
            BigDecimal expectedAmount,
            Instant since
    ) {
        return switch (network) {
            case BTC -> checkBtcPayment(address, expectedAmount, since);
            case ETH -> checkEthPayment(address, expectedAmount, since);
            case SOL -> checkSolPayment(address, expectedAmount, since);
            case TON -> checkTonPayment(address, expectedAmount, since);
            case USDT_TON -> checkTonUsdtPayment(address, expectedAmount, since);
            case USDT_TRC20 -> checkTrc20Payment(address, expectedAmount, since);
            case XAUT -> checkEthTokenPayment(
                    address, expectedAmount, since,
                    "0x68749665FF8D2d112Fa859AA293F07A622782F38", 6
            );
        };
    }

    @SuppressWarnings("unchecked")
    private boolean checkBtcPayment(String address, BigDecimal expectedAmount, Instant since) {
        try {
            var txs = blockchainRestClient.get()
                    .uri("https://blockstream.info/api/address/{address}/txs", address)
                    .retrieve()
                    .body(List.class);

            if (txs == null) return false;

            var expectedSats = expectedAmount.movePointRight(8).longValue();
            var sinceEpoch = since.getEpochSecond();

            for (Object txObj : txs) {
                var tx = (Map<String, Object>) txObj;
                var status = (Map<String, Object>) tx.get("status");
                if (status == null || !Boolean.TRUE.equals(status.get("confirmed"))) continue;

                var blockTime = (Number) status.get("block_time");
                if (blockTime != null && blockTime.longValue() < sinceEpoch) continue;

                var vouts = (List<Map<String, Object>>) tx.get("vout");
                if (vouts == null) continue;

                for (var vout : vouts) {
                    var scriptAddr = (String) vout.get("scriptpubkey_address");
                    var value = (Number) vout.get("value");
                    if (address.equals(scriptAddr) && value != null && value.longValue() == expectedSats) {
                        log.info("BTC payment found: {} sats to {}", value, address);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check BTC blockchain: {}", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkEthPayment(String address, BigDecimal expectedAmount, Instant since) {
        try {
            var result = blockchainRestClient.get()
                    .uri("https://api.etherscan.io/api?module=account&action=txlist&address={address}&startblock=0&endblock=99999999&sort=desc&page=1&offset=20",
                            address)
                    .retrieve()
                    .body(Map.class);

            if (result == null || !"1".equals(String.valueOf(result.get("status")))) return false;

            var txs = (List<Map<String, Object>>) result.get("result");
            if (txs == null) return false;

            var expectedWei = expectedAmount.movePointRight(18);
            var sinceEpoch = since.getEpochSecond();

            for (var tx : txs) {
                var timestamp = Long.parseLong(String.valueOf(tx.get("timeStamp")));
                if (timestamp < sinceEpoch) continue;

                var to = String.valueOf(tx.get("to"));
                var value = String.valueOf(tx.get("value"));

                if (address.equalsIgnoreCase(to) && new BigDecimal(value).compareTo(expectedWei) == 0) {
                    log.info("ETH payment found: {} wei to {}", value, address);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check ETH blockchain: {}", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkSolPayment(String address, BigDecimal expectedAmount, Instant since) {
        try {
            var result = blockchainRestClient.get()
                    .uri("https://api.solscan.io/v2/account/transactions?address={address}&limit=20", address)
                    .retrieve()
                    .body(Map.class);

            if (result == null) return false;

            var data = (List<Map<String, Object>>) result.get("data");
            if (data == null) return false;

            var expectedLamports = expectedAmount.movePointRight(9);
            var sinceEpoch = since.getEpochSecond();

            for (var tx : data) {
                var blockTime = (Number) tx.get("block_time");
                if (blockTime != null && blockTime.longValue() < sinceEpoch) continue;

                var lamport = tx.get("lamport");
                if (lamport != null) {
                    var amount = new BigDecimal(String.valueOf(lamport));
                    if (amount.compareTo(expectedLamports) == 0) {
                        log.info("SOL payment found: {} lamports to {}", lamport, address);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check SOL blockchain: {}", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkEthTokenPayment(
            String address,
            BigDecimal expectedAmount,
            Instant since,
            String contractAddress,
            int decimals
    ) {
        try {
            var result = blockchainRestClient.get()
                    .uri("https://api.etherscan.io/api?module=account&action=tokentx&contractaddress={contract}&address={address}&sort=desc&page=1&offset=20",
                            contractAddress, address)
                    .retrieve()
                    .body(Map.class);

            if (result == null || !"1".equals(String.valueOf(result.get("status")))) return false;

            var txs = (List<Map<String, Object>>) result.get("result");
            if (txs == null) return false;

            var expectedRaw = expectedAmount.movePointRight(decimals);
            var sinceEpoch = since.getEpochSecond();

            for (var tx : txs) {
                var timestamp = Long.parseLong(String.valueOf(tx.get("timeStamp")));
                if (timestamp < sinceEpoch) continue;

                var to = String.valueOf(tx.get("to"));
                var value = String.valueOf(tx.get("value"));

                if (address.equalsIgnoreCase(to) && new BigDecimal(value).compareTo(expectedRaw) == 0) {
                    log.info("ERC-20 token payment found: {} to {}", expectedAmount, address);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check ERC-20 token: {}", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkTrc20Payment(String address, BigDecimal expectedAmount, Instant since) {
        try {
            var contract = cryptoProperties.getNetworks()
                    .getOrDefault("usdt-trc20", new CryptoProperties.NetworkProperties())
                    .getContract();

            var result = blockchainRestClient.get()
                    .uri("https://api.trongrid.io/v1/accounts/{address}/transactions/trc20?limit=50&contract_address={contract}",
                            address, contract)
                    .retrieve()
                    .body(Map.class);

            if (result == null) return false;

            var data = (List<Map<String, Object>>) result.get("data");
            if (data == null) return false;

            var expectedRaw = expectedAmount.movePointRight(6);
            var sinceMs = since.toEpochMilli();

            for (var tx : data) {
                var timestamp = (Number) tx.get("block_timestamp");
                if (timestamp != null && timestamp.longValue() < sinceMs) continue;

                var to = String.valueOf(tx.get("to"));
                var value = String.valueOf(tx.get("value"));

                if (address.equalsIgnoreCase(to) && new BigDecimal(value).compareTo(expectedRaw) == 0) {
                    log.info("USDT TRC-20 payment found: {} to {}", expectedAmount, address);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check TRC-20 blockchain: {}", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkTonPayment(String address, BigDecimal expectedAmount, Instant since) {
        try {
            var result = blockchainRestClient.get()
                    .uri("https://tonapi.io/v2/accounts/{address}/events?limit=20", address)
                    .retrieve()
                    .body(Map.class);

            if (result == null) return false;

            var events = (List<Map<String, Object>>) result.get("events");
            if (events == null) return false;

            var expectedNano = expectedAmount.movePointRight(9);
            var sinceEpoch = since.getEpochSecond();

            for (var event : events) {
                var timestamp = (Number) event.get("timestamp");
                if (timestamp != null && timestamp.longValue() < sinceEpoch) continue;

                var actions = (List<Map<String, Object>>) event.get("actions");
                if (actions == null) continue;

                for (var action : actions) {
                    if (!"TonTransfer".equals(action.get("type"))) continue;

                    var tonTransfer = (Map<String, Object>) action.get("TonTransfer");
                    if (tonTransfer == null) continue;

                    var amount = String.valueOf(tonTransfer.get("amount"));
                    log.debug("TON event: amount={}, expected={}", amount, expectedNano);

                    if (new BigDecimal(amount).compareTo(expectedNano) == 0) {
                        log.info("TON payment found: {} nanoton to {}", amount, address);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check TON blockchain: {}", e.getMessage());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkTonUsdtPayment(String address, BigDecimal expectedAmount, Instant since) {
        try {
            var contract = cryptoProperties.getNetworks()
                    .getOrDefault("usdt-ton", new CryptoProperties.NetworkProperties())
                    .getContract();

            var result = blockchainRestClient.get()
                    .uri("https://tonapi.io/v2/accounts/{address}/jettons/{jetton}/history?limit=20",
                            address, contract)
                    .retrieve()
                    .body(Map.class);

            if (result == null) return false;

            var events = (List<Map<String, Object>>) result.get("events");
            if (events == null) return false;

            var expectedRaw = expectedAmount.movePointRight(6);
            var sinceEpoch = since.getEpochSecond();

            for (var event : events) {
                var timestamp = (Number) event.get("timestamp");
                if (timestamp != null && timestamp.longValue() < sinceEpoch) continue;

                var actions = (List<Map<String, Object>>) event.get("actions");
                if (actions == null) continue;

                for (var action : actions) {
                    if (!"JettonTransfer".equals(action.get("type"))) continue;

                    var jettonTransfer = (Map<String, Object>) action.get("JettonTransfer");
                    if (jettonTransfer == null) continue;

                    var amount = String.valueOf(jettonTransfer.get("amount"));
                    log.debug("USDT TON event: amount={}, expected={}", amount, expectedRaw);

                    if (new BigDecimal(amount).compareTo(expectedRaw) == 0) {
                        log.info("USDT TON payment found: {} to {}", expectedAmount, address);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check USDT TON blockchain: {}", e.getMessage());
        }
        return false;
    }
}
