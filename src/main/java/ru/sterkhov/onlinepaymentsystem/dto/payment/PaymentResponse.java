package ru.sterkhov.onlinepaymentsystem.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import ru.sterkhov.onlinepaymentsystem.config.AppProperties;
import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Ответ с информацией о платеже.
 */
@Getter
@ToString
@Builder
public class PaymentResponse {

    /**
     * Создаёт ответ из сущности платежа и настроек приложения.
     *
     * @param payment       сущность платежа
     * @param appProperties настройки приложения
     * @return заполненный ответ о платеже
     */
    public static PaymentResponse from(Payment payment, AppProperties appProperties) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .providerName(payment.getProviderName())
                .paymentUrl(appProperties.paymentUrl(payment.getId().toString()))
                .metadata(payment.getMetadata())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .paidAt(payment.getPaidAt())
                .expiresAt(payment.getExpiresAt())
                .build();
    }

    /** Идентификатор платежа. */
    private final UUID id;

    /** Идентификатор заказа. */
    private final String orderId;

    /** Сумма платежа. */
    private final BigDecimal amount;

    /** Валюта платежа. */
    private final String currency;

    /** Статус платежа. */
    private final PaymentStatus status;

    /** Метод оплаты. */
    private final PaymentMethod method;

    /** Имя провайдера. */
    private final String providerName;

    /** URL страницы оплаты. */
    private final String paymentUrl;

    /** Метаданные платежа. */
    private final Map<String, Object> metadata;

    /** Дата создания. */
    private final Instant createdAt;

    /** Дата последнего обновления. */
    private final Instant updatedAt;

    /** Дата оплаты. */
    private final Instant paidAt;

    /** Дата истечения срока. */
    private final Instant expiresAt;
}
