package ru.sterkhov.onlinepaymentsystem.dto.tbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Запрос на создание счёта в API Т-Банка.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TBankInvoiceRequest(
        /** Номер счёта. */
        String invoiceNumber,
        /** Дата окончания срока оплаты. */
        String dueDate,
        /** Дата выставления счёта. */
        String invoiceDate,
        /** Номер расчётного счёта. */
        String accountNumber,
        /** Плательщик. */
        Payer payer,
        /** Позиции счёта. */
        List<Item> items,
        /** Контакты для уведомлений. */
        List<Contact> contacts,
        /** Комментарий к счёту. */
        String comment,
        /** Назначение платежа. */
        String customPaymentPurpose
) {

    /**
     * Данные плательщика в счёте Т-Банка.
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payer(
            /** Наименование плательщика. */
            String name,
            /** ИНН плательщика. */
            String inn,
            /** КПП плательщика. */
            String kpp
    ) {
    }

    /**
     * Позиция в счёте Т-Банка.
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            /** Наименование товара/услуги. */
            String name,
            /** Цена за единицу. */
            double price,
            /** Единица измерения. */
            String unit,
            /** Ставка НДС. */
            String vat,
            /** Количество. */
            int amount
    ) {
    }

    /**
     * Контактные данные для уведомлений по счёту.
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Contact(
            /** Email для уведомлений. */
            String email,
            /** Телефон для уведомлений. */
            String phone
    ) {
    }
}
