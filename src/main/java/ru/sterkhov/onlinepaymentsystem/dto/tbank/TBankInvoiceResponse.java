package ru.sterkhov.onlinepaymentsystem.dto.tbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ответ на создание счёта от API Т-Банка.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TBankInvoiceResponse(
        /** Идентификатор созданного счёта. */
        String invoiceId
) {
}
