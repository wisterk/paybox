package me.wisterk.paybox.domain.enums;

/**
 * Статусы жизненного цикла платежа.
 */
public enum PaymentStatus {

    /** Платёж создан, ожидает выбора метода оплаты. */
    CREATED,

    /** Платёж ожидает подтверждения оплаты. */
    PENDING,

    /** Платёж успешно оплачен. */
    PAID,

    /** Платёж завершён с ошибкой. */
    FAILED,

    /** Срок платежа истёк. */
    EXPIRED,

    /** Платёж отменён. */
    CANCELLED,

    /** Платёж возвращён. */
    REFUNDED,
    ;

    /**
     * Определяет, находится ли платёж в терминальном (финальном) состоянии.
     *
     * @return {@code true}, если статус терминальный
     */
    public boolean isTerminal() {
        return this == PAID || this == FAILED || this == EXPIRED
                || this == CANCELLED || this == REFUNDED;
    }
}
