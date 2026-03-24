package me.wisterk.paybox.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import me.wisterk.paybox.config.AppProperties;
import me.wisterk.paybox.config.DonationProperties;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Сервис OAuth-авторизации через DonationAlerts.
 */
@Service
@RequiredArgsConstructor
@ToString
@Slf4j
public class DonationAlertsOAuthService {

    /** Ключ access-токена в Redis. */
    private static final String TOKEN_KEY = "da:access_token";

    /** Ключ refresh-токена в Redis. */
    private static final String REFRESH_KEY = "da:refresh_token";

    /** Настройки провайдеров донатов. */
    private final DonationProperties donationProperties;

    /** Настройки приложения. */
    private final AppProperties appProperties;

    /** Шаблон для работы с Redis. */
    private final RedisTemplate<String, String> redisTemplate;

    /** HTTP-клиент для OAuth-запросов. */
    private final RestClient httpClient = RestClient.builder()
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();

    /**
     * Инициализирует OAuth-токены при запуске приложения.
     */
    @PostConstruct
    public void init() {
        var da = donationProperties.getDonationAlerts();
        if (!da.isEnabled()) return;

        if (da.getClientId() == null || da.getClientId().isBlank()) return;

        var existing = redisTemplate.opsForValue().get(REFRESH_KEY);
        if (existing != null) {
            log.info("DonationAlerts: refresh token found in Redis, auto-refreshing access token...");
            var token = refreshAccessToken(existing);
            if (token != null) {
                log.info("DonationAlerts: ready");
                return;
            }
        }

        if (da.getRefreshToken() != null && !da.getRefreshToken().isBlank()) {
            log.info("DonationAlerts: using refresh token from config...");
            var token = refreshAccessToken(da.getRefreshToken());
            if (token != null) {
                log.info("DonationAlerts: ready");
                return;
            }
        }

        log.warn("========================================================");
        log.warn("DonationAlerts: OAuth не настроен!");
        log.warn("Перейдите по ссылке для авторизации (один раз):");
        log.warn("{}/oauth/donationalerts/connect", appProperties.getBaseUrl());
        log.warn("========================================================");
    }

    /**
     * Возвращает валидный access-токен, обновляя при необходимости.
     *
     * @return access-токен или {@code null}, если авторизация не настроена
     */
    public String getAccessToken() {
        var cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null) {
            return cached;
        }

        var refreshToken = redisTemplate.opsForValue().get(REFRESH_KEY);
        if (refreshToken == null) {
            refreshToken = donationProperties.getDonationAlerts().getRefreshToken();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("DonationAlerts: no refresh token available. Complete OAuth flow first.");
            return null;
        }

        return refreshAccessToken(refreshToken);
    }

    /**
     * Формирует URL для начала OAuth-авторизации.
     *
     * @return URL авторизации
     */
    public String getAuthorizationUrl() {
        var da = donationProperties.getDonationAlerts();
        var redirectUri = enc(appProperties.getBaseUrl() + "/oauth/donationalerts/callback");
        return "https://www.donationalerts.com/oauth/authorize"
                + "?client_id=" + da.getClientId()
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=oauth-donation-index";
    }

    private String callbackUri() {
        return appProperties.getBaseUrl() + "/oauth/donationalerts/callback";
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Обменивает код авторизации на токены.
     *
     * @param code код авторизации
     * @return access-токен или {@code null} при ошибке
     */
    @SuppressWarnings("unchecked")
    public String exchangeCode(String code) {
        var da = donationProperties.getDonationAlerts();
        try {
            var body = "grant_type=authorization_code"
                    + "&client_id=" + enc(da.getClientId())
                    + "&client_secret=" + enc(da.getClientSecret())
                    + "&code=" + enc(code)
                    + "&redirect_uri=" + enc(callbackUri());

            log.info("DonationAlerts: exchanging code for tokens...");
            var response = httpClient.post()
                    .uri("https://www.donationalerts.com/oauth/token")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return storeTokens(response);
        } catch (Exception e) {
            log.error("DonationAlerts OAuth code exchange failed: {}", e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String refreshAccessToken(String refreshToken) {
        var da = donationProperties.getDonationAlerts();
        try {
            var body = "grant_type=refresh_token"
                    + "&client_id=" + enc(da.getClientId())
                    + "&client_secret=" + enc(da.getClientSecret())
                    + "&refresh_token=" + enc(refreshToken);

            var response = httpClient.post()
                    .uri("https://www.donationalerts.com/oauth/token")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return storeTokens(response);
        } catch (Exception e) {
            log.error("DonationAlerts token refresh failed: {}", e.getMessage());
            return null;
        }
    }

    private String storeTokens(Map<String, Object> response) {
        if (response == null) return null;

        var accessToken = (String) response.get("access_token");
        var newRefreshToken = (String) response.get("refresh_token");
        var expiresIn = (Number) response.get("expires_in");

        if (accessToken != null) {
            var ttl = expiresIn != null ? expiresIn.longValue() - 60 : 3500;
            redisTemplate.opsForValue().set(TOKEN_KEY, accessToken, Duration.ofSeconds(ttl));
            log.info("DonationAlerts access token refreshed, expires in {}s", ttl);
        }

        if (newRefreshToken != null) {
            redisTemplate.opsForValue().set(REFRESH_KEY, newRefreshToken);
        }

        return accessToken;
    }
}
