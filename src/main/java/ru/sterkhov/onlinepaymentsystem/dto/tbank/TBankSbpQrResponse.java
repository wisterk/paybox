package ru.sterkhov.onlinepaymentsystem.dto.tbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ответ на создание СБП QR-кода от API Т-Банка.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TBankSbpQrResponse(
        /** Идентификатор QR-кода. */
        String qrId,
        /** Данные QR-кода. */
        String data,
        /** URL для оплаты. */
        String paymentUrl
) {
}
