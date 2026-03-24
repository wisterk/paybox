package ru.sterkhov.onlinepaymentsystem.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.sterkhov.onlinepaymentsystem.config.AppProperties;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.dto.payment.CreatePaymentRequest;
import ru.sterkhov.onlinepaymentsystem.dto.payment.PaymentResponse;
import ru.sterkhov.onlinepaymentsystem.service.PaymentService;
import ru.sterkhov.onlinepaymentsystem.service.provider.PaymentProviderFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST-контроллер для управления платежами через API.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@ToString
@Tag(name = "Payments", description = "Payment management API")
@SecurityRequirement(name = "apiKey")
public class PaymentApiController {

    /** Сервис платежей. */
    private final PaymentService paymentService;

    /** Фабрика провайдеров. */
    private final PaymentProviderFactory providerFactory;

    /** Настройки приложения. */
    private final AppProperties appProperties;

    /**
     * Создаёт новый платёж.
     *
     * @param request данные для создания платежа
     * @return информация о созданном платеже
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new payment. Returns paymentUrl for user redirect.")
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        var payment = paymentService.createPayment(request);
        return PaymentResponse.from(payment, appProperties);
    }

    /**
     * Возвращает платёж по идентификатору.
     *
     * @param id идентификатор платежа
     * @return информация о платеже
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.getPayment(id), appProperties);
    }

    /**
     * Обновляет статус платежа из провайдера.
     *
     * @param id идентификатор платежа
     * @return обновлённая информация о платеже
     */
    @PostMapping("/{id}/refresh")
    @Operation(summary = "Refresh payment status from provider")
    public PaymentResponse refreshStatus(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.refreshStatus(id), appProperties);
    }

    /**
     * Возвращает список платежей по идентификатору заказа.
     *
     * @param orderId идентификатор заказа
     * @return список платежей
     */
    @GetMapping("/by-order/{orderId}")
    @Operation(summary = "Get payments by order ID")
    public List<PaymentResponse> getByOrderId(@PathVariable String orderId) {
        return paymentService.findByOrderId(orderId).stream()
                .map(p -> PaymentResponse.from(p, appProperties))
                .toList();
    }

    /**
     * Возвращает набор доступных методов оплаты.
     *
     * @return набор доступных методов
     */
    @GetMapping("/methods")
    @Operation(summary = "Get available payment methods")
    public Set<PaymentMethod> getAvailableMethods() {
        return providerFactory.getAvailableMethods();
    }
}
