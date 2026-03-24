package me.wisterk.paybox.dto.tbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ответ со статусом СБП QR-кода от API Т-Банка.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TBankSbpQrStatusResponse(
        /** Идентификатор QR-кода. */
        String qrId,
        /** Статус QR-кода. */
        String status
) {
}
