package ru.sterkhov.onlinepaymentsystem.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки провайдеров донатов (DonationAlerts, DonatePay).
 */
@Component
@ConfigurationProperties(prefix = "payment.providers.donation")
@Getter
@Setter
@ToString
public class DonationProperties {

    /** Настройки DonationAlerts. */
    private DonationAlertsProps donationAlerts = new DonationAlertsProps();

    /** Настройки DonatePay. */
    private DonatePayProps donatePay = new DonatePayProps();

    /**
     * Настройки интеграции с DonationAlerts.
     */
    @Getter
    @Setter
    @ToString
    public static class DonationAlertsProps {

        /** Включена ли интеграция. */
        private boolean enabled;

        /** OAuth client ID. */
        private String clientId;

        /** OAuth client secret. */
        private String clientSecret;

        /** OAuth refresh token. */
        private String refreshToken;

        /** Имя страницы для ссылки на оплату. */
        private String pageName;
    }

    /**
     * Настройки интеграции с DonatePay.
     */
    @Getter
    @Setter
    @ToString
    public static class DonatePayProps {

        /** Включена ли интеграция. */
        private boolean enabled;

        /** API-ключ DonatePay. */
        private String apiKey;

        /** Имя страницы для ссылки на оплату. */
        private String pageName;
    }
}
