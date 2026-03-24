package ru.sterkhov.onlinepaymentsystem.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Сущность платежа, хранимая в базе данных.
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Payment {

    /** Уникальный идентификатор платежа. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Идентификатор заказа. */
    private String orderId;

    /** Сумма платежа. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Валюта платежа. */
    @Column(nullable = false, length = 10)
    private String currency = "RUB";

    /** Текущий статус платежа. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status = PaymentStatus.CREATED;

    /** Метод оплаты. */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PaymentMethod method;

    /** Имя платёжного провайдера. */
    @Column(length = 64)
    private String providerName;

    /** Внешний идентификатор в системе провайдера. */
    private String externalId;

    /** URL для оплаты. */
    private String paymentUrl;

    /** URL для редиректа после оплаты. */
    private String redirectUrl;

    /** Метаданные платежа в формате JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /** Версия для оптимистичной блокировки. */
    @Version
    private Integer version;

    /** Дата и время создания платежа. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Дата и время последнего обновления. */
    @Column(nullable = false)
    private Instant updatedAt;

    /** Дата и время оплаты. */
    private Instant paidAt;

    /** Дата и время истечения срока. */
    private Instant expiresAt;

    /**
     * Устанавливает временные метки при создании записи.
     */
    @PrePersist
    void prePersist() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Обновляет временную метку при изменении записи.
     */
    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
