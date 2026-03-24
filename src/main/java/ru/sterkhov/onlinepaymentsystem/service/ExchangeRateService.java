package ru.sterkhov.onlinepaymentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.sterkhov.onlinepaymentsystem.domain.enums.CryptoNetwork;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис получения курсов криптовалют к рублю.
 */
@Service
@RequiredArgsConstructor
@ToString
@Slf4j
public class ExchangeRateService {

    /** Ключ кэша курсов в Redis. */
    private static final String CACHE_KEY = "exchange:rates:rub";

    /** Время жизни кэша курсов. */
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    /** URL API CoinGecko для получения курсов. */
    private static final String COINGECKO_URL =
            "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,the-open-network,tether,tether-gold&vs_currencies=rub";

    /** Шаблон для работы с Redis. */
    private final RedisTemplate<String, String> redisTemplate;

    /** HTTP-клиент для запросов к CoinGecko. */
    private final RestClient httpClient = RestClient.builder()
            .defaultHeader("Accept", "application/json")
            .build();

    /**
     * Конвертирует сумму в рублях в криптовалюту.
     *
     * @param amountRub сумма в рублях
     * @param network   криптовалютная сеть
     * @return сумма в криптовалюте или {@code null}, если курс недоступен
     */
    public BigDecimal convert(BigDecimal amountRub, CryptoNetwork network) {
        var rate = getRate(network);
        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return amountRub.divide(rate, network.getDecimals(), RoundingMode.HALF_UP);
    }

    /**
     * Возвращает курс криптовалюты к рублю.
     *
     * @param network криптовалютная сеть
     * @return курс (рублей за 1 единицу) или {@code null}
     */
    public BigDecimal getRate(CryptoNetwork network) {
        var rates = getRates();
        return rates.get(network);
    }

    /**
     * Возвращает все текущие курсы криптовалют к рублю.
     *
     * @return карта сеть-курс
     */
    public Map<CryptoNetwork, BigDecimal> getRates() {
        var cached = redisTemplate.opsForHash().entries(CACHE_KEY);
        if (cached != null && !cached.isEmpty()) {
            return deserializeRates(cached);
        }
        return fetchAndCacheRates();
    }

    @SuppressWarnings("unchecked")
    private Map<CryptoNetwork, BigDecimal> fetchAndCacheRates() {
        try {
            var response = httpClient.get()
                    .uri(COINGECKO_URL)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return Map.of();

            var rates = new EnumMap<CryptoNetwork, BigDecimal>(CryptoNetwork.class);

            extractRate(response, "bitcoin", CryptoNetwork.BTC, rates);
            extractRate(response, "ethereum", CryptoNetwork.ETH, rates);
            extractRate(response, "solana", CryptoNetwork.SOL, rates);
            extractRate(response, "the-open-network", CryptoNetwork.TON, rates);
            extractRate(response, "tether", CryptoNetwork.USDT_TRC20, rates);
            extractRate(response, "tether", CryptoNetwork.USDT_TON, rates);
            extractRate(response, "tether-gold", CryptoNetwork.XAUT, rates);

            var hash = new HashMap<String, String>();
            rates.forEach((k, v) -> hash.put(k.name(), v.toPlainString()));
            redisTemplate.opsForHash().putAll(CACHE_KEY, hash);
            redisTemplate.expire(CACHE_KEY, CACHE_TTL);

            log.debug("Exchange rates fetched: {}", rates);
            return rates;
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rates: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private void extractRate(
            Map<String, Object> response,
            String coinId,
            CryptoNetwork network,
            Map<CryptoNetwork, BigDecimal> rates
    ) {
        var coin = (Map<String, Object>) response.get(coinId);
        if (coin != null && coin.get("rub") != null) {
            rates.put(network, new BigDecimal(coin.get("rub").toString()));
        }
    }

    private Map<CryptoNetwork, BigDecimal> deserializeRates(Map<Object, Object> cached) {
        var rates = new EnumMap<CryptoNetwork, BigDecimal>(CryptoNetwork.class);
        cached.forEach((k, v) -> {
            try {
                rates.put(CryptoNetwork.valueOf(k.toString()), new BigDecimal(v.toString()));
            } catch (Exception ignored) {
            }
        });
        return rates;
    }
}
