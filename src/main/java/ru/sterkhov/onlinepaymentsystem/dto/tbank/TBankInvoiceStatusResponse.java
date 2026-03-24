package ru.sterkhov.onlinepaymentsystem.dto.tbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ответ со статусом счёта от API Т-Банка.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TBankInvoiceStatusResponse(
        /** Идентификатор счёта. */
        String invoiceId,
        /** Статус счёта. */
        String status,
        /** Номер счёта. */
        String invoiceNumber
) {
}
