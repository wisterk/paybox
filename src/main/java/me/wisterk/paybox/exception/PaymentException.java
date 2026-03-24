package me.wisterk.paybox.exception;

import lombok.experimental.StandardException;

/**
 * Исключение, возникающее при ошибках обработки платежа.
 */
@StandardException
public class PaymentException extends RuntimeException {
}
