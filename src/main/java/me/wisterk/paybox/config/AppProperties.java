package me.wisterk.paybox.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Общие настройки приложения.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@ToString
public class AppProperties {

    /** Базовый URL приложения. */
    private String baseUrl = "http://localhost:8080";

    /** Настройки песочницы. */
    private SandboxProperties sandbox = new SandboxProperties();

    /**
     * Формирует URL страницы оплаты для указанного платежа.
     *
     * @param paymentId идентификатор платежа
     * @return полный URL страницы оплаты
     */
    public String paymentUrl(String paymentId) {
        return baseUrl + "/pay/" + paymentId;
    }

    /**
     * Настройки режима песочницы.
     */
    @Getter
    @Setter
    @ToString
    public static class SandboxProperties {

        /** Включён ли режим песочницы. */
        private boolean enabled = true;
    }
}
