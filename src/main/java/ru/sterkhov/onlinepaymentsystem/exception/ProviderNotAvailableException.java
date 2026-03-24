package ru.sterkhov.onlinepaymentsystem.exception;

import lombok.experimental.StandardException;

/**
 * Исключение, возникающее при недоступности платёжного провайдера.
 */
@StandardException
public class ProviderNotAvailableException extends RuntimeException {
}
