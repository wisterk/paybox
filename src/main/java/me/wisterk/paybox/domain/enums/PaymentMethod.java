package me.wisterk.paybox.domain.enums;

/**
 * Доступные методы оплаты.
 */
public enum PaymentMethod {

    /** Выставление счёта через Т-Банк. */
    INVOICE,

    /** СБП QR-код через Т-Банк. */
    SBP_QR,

    /** Оплата банковской картой. */
    CARD,

    /** Оплата криптовалютой. */
    CRYPTO,

    /** Оплата через DonationAlerts. */
    DONATION_ALERTS,

    /** Оплата через DonatePay. */
    DONATE_PAY,
}
