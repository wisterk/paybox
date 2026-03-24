package me.wisterk.paybox.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.wisterk.paybox.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Запрос на создание нового платежа.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /** Сумма платежа. */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    /** Валюта платежа (по умолчанию RUB). */
    private String currency;

    /** Метод оплаты. */
    private PaymentMethod method;

    /** Идентификатор заказа. */
    private String orderId;

    /** Описание платежа. */
    private String description;

    /** URL для редиректа после оплаты. */
    private String redirectUrl;

    /** Криптовалютная сеть (для крипто-платежей). */
    private String cryptoNetwork;

    /** Данные плательщика (для счетов). */
    private InvoicePayer payer;

    /** Позиции счёта. */
    private List<InvoiceItem> items;

    /** Комментарий к платежу. */
    private String comment;

    /** Контактные email-адреса. */
    private List<String> contactEmails;

    /** Контактные телефоны. */
    private List<String> contactPhones;

    /**
     * Данные плательщика для выставления счёта.
     */
    @Getter
    @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoicePayer {

        /** Наименование плательщика. */
        private String name;

        /** ИНН плательщика. */
        private String inn;

        /** КПП плательщика. */
        private String kpp;
    }

    /**
     * Позиция в счёте.
     */
    @Getter
    @Setter
    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItem {

        /** Наименование товара/услуги. */
        private String name;

        /** Цена за единицу. */
        private double price;

        /** Единица измерения. */
        private String unit;

        /** Ставка НДС. */
        private String vat;

        /** Количество. */
        private int amount;
    }
}
