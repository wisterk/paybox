package ru.sterkhov.onlinepaymentsystem.dto.tbank;

/**
 * Ответ с информацией об ошибке от API Т-Банка.
 */
public record TBankErrorResponse(
        /** Идентификатор ошибки. */
        String errorId,
        /** Текст сообщения об ошибке. */
        String errorMessage,
        /** Код ошибки. */
        String errorCode
) {
}
