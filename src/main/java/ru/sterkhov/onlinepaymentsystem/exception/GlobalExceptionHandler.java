package ru.sterkhov.onlinepaymentsystem.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.sterkhov.onlinepaymentsystem.dto.payment.ErrorResponse;

/**
 * Глобальный обработчик исключений для REST-контроллеров.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Обрабатывает ошибки платежей.
     *
     * @param e исключение платежа
     * @return ответ с сообщением об ошибке
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException e) {
        log.warn("Payment error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().message(e.getMessage()).build());
    }

    /**
     * Обрабатывает недоступность провайдера.
     *
     * @param e исключение недоступности провайдера
     * @return ответ с кодом 503
     */
    @ExceptionHandler(ProviderNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleProviderNotAvailable(ProviderNotAvailableException e) {
        log.warn("Provider not available: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.builder().message(e.getMessage()).build());
    }

    /**
     * Обрабатывает ошибки валидации запросов.
     *
     * @param e исключение валидации
     * @return ответ с описанием ошибок валидации
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        var message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder().message(message).build());
    }

    /**
     * Обрабатывает ошибки «не найдено».
     *
     * @param e исключение отсутствия ресурса
     * @return ответ с кодом 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder().message("Not found").build());
    }

    /**
     * Обрабатывает все прочие непредвиденные исключения.
     *
     * @param e непредвиденное исключение
     * @return ответ с кодом 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.builder().message("Internal server error").build());
    }
}
