package ru.sterkhov.onlinepaymentsystem.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки подключения к API Т-Банка.
 */
@Component
@ConfigurationProperties(prefix = "payment.providers.tbank")
@Getter
@Setter
@ToString
public class TBankProperties {

    /** Включён ли провайдер Т-Банка. */
    private boolean enabled;

    /** Bearer-токен для авторизации в API. */
    private String bearerToken;

    /** Базовый URL API Т-Банка. */
    private String baseUrl;

    /** Номер расчётного счёта. */
    private String accountNumber;

    /** Секрет для верификации вебхуков. */
    private String webhookSecret;

    /** Тайм-аут подключения в миллисекундах. */
    private int connectTimeout = 5000;

    /** Тайм-аут чтения в миллисекундах. */
    private int readTimeout = 10000;

    /** Настройки выставления счетов. */
    private InvoiceProperties invoice = new InvoiceProperties();

    /** Настройки СБП. */
    private SbpProperties sbp = new SbpProperties();

    /**
     * Настройки выставления счетов через Т-Банк.
     */
    @Getter
    @Setter
    @ToString
    public static class InvoiceProperties {

        /** Включено ли выставление счетов. */
        private boolean enabled;
    }

    /**
     * Настройки СБП через Т-Банк.
     */
    @Getter
    @Setter
    @ToString
    public static class SbpProperties {

        /** Включена ли оплата через СБП. */
        private boolean enabled;
    }
}
