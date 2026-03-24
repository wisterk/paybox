package ru.sterkhov.onlinepaymentsystem.dto.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Стандартный ответ об ошибке.
 */
@Getter
@ToString
@Builder
public class ErrorResponse {

    /** Текст сообщения об ошибке. */
    private final String message;

    /** Временная метка ошибки. */
    @Builder.Default
    private final Instant timestamp = Instant.now();
}
