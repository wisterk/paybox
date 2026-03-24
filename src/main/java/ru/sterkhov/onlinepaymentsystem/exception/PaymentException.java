package ru.sterkhov.onlinepaymentsystem.exception;

import lombok.experimental.StandardException;

/**
 * Исключение, возникающее при ошибках обработки платежа.
 */
@StandardException
public class PaymentException extends RuntimeException {
}
