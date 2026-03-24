package me.wisterk.paybox.dto.tbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Запрос на создание СБП QR-кода через API Т-Банка.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TBankSbpQrRequest(
        /** Номер расчётного счёта. */
        String accountNumber,
        /** Сумма платежа. */
        double sum,
        /** Назначение платежа. */
        String purpose,
        /** Ставка НДС. */
        String vat,
        /** URL для редиректа после оплаты. */
        String redirectUrl,
        /** Время жизни QR-кода в минутах. */
        Integer ttl
) {
}
