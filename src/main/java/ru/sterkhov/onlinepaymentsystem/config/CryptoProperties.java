package ru.sterkhov.onlinepaymentsystem.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Настройки криптовалютного провайдера.
 */
@Component
@ConfigurationProperties(prefix = "payment.providers.crypto")
@Getter
@Setter
@ToString
public class CryptoProperties {

    /** Включён ли криптовалютный провайдер. */
    private boolean enabled;

    /** Время жизни крипто-платежа в минутах. */
    private int paymentTtlMinutes = 60;

    /** Настройки отдельных криптовалютных сетей. */
    private Map<String, NetworkProperties> networks = new HashMap<>();

    /**
     * Настройки конкретной криптовалютной сети.
     */
    @Getter
    @Setter
    @ToString
    public static class NetworkProperties {

        /** Включена ли сеть. */
        private boolean enabled;

        /** Адрес кошелька для приёма платежей. */
        private String address;

        /** Необходимое количество подтверждений. */
        private int confirmations = 1;

        /** Адрес смарт-контракта токена. */
        private String contract;
    }
}
