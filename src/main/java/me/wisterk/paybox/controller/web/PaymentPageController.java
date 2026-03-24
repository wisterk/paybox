package me.wisterk.paybox.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.wisterk.paybox.domain.entity.Payment;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import me.wisterk.paybox.config.AppProperties;
import me.wisterk.paybox.domain.enums.CryptoNetwork;
import me.wisterk.paybox.dto.payment.ConfirmPaymentRequest;
import me.wisterk.paybox.dto.payment.ErrorResponse;
import me.wisterk.paybox.dto.payment.PaymentResponse;
import me.wisterk.paybox.exception.PaymentException;
import me.wisterk.paybox.service.ExchangeRateService;
import me.wisterk.paybox.service.PaymentService;
import me.wisterk.paybox.service.provider.CryptoProvider;
import me.wisterk.paybox.service.provider.PaymentProviderFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер веб-страниц оплаты.
 */
@Controller
@RequiredArgsConstructor
@ToString
public class PaymentPageController {

    /** Сервис платежей. */
    private final PaymentService paymentService;

    /** Фабрика провайдеров. */
    private final PaymentProviderFactory providerFactory;

    /** Криптовалютный провайдер. */
    private final CryptoProvider cryptoProvider;

    /** Настройки приложения. */
    private final AppProperties appProperties;

    /** Сервис курсов валют. */
    private final ExchangeRateService exchangeRateService;

    /** Окружение Spring. */
    private final Environment env;

    /**
     * Корневой маршрут приложения.
     *
     * @return редирект на песочницу или страницу ошибки
     */
    @GetMapping("/")
    public String root() {
        if (appProperties.getSandbox().isEnabled()) {
            return "redirect:/sandbox";
        }
        return "redirect:/pay/invalid";
    }

    /**
     * Редирект с /pay на песочницу.
     *
     * @return URL редиректа
     */
    @GetMapping("/pay")
    public String payRedirect() {
        return "redirect:/sandbox";
    }

    /**
     * Отображает страницу статуса платежа или выбора метода оплаты.
     *
     * @param id    идентификатор платежа
     * @param model модель для шаблона
     * @return имя шаблона
     */
    @GetMapping("/pay/{id}")
    public String paymentStatus(@PathVariable String id, Model model) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorTitle", "Некорректная ссылка");
            model.addAttribute("errorMessage", "Ссылка на платёж невалидна. Проверьте URL или обратитесь к продавцу.");
            return "error/payment-error";
        }

        var optPayment = paymentService.findPayment(uuid);
        if (optPayment.isEmpty()) {
            model.addAttribute("errorTitle", "Платёж не найден");
            model.addAttribute("errorMessage", "Платёж с таким ID не существует. Возможно, ссылка устарела.");
            return "error/payment-error";
        }

        var payment = optPayment.get();

        if (payment.getMethod() == null) {
            model.addAttribute("payment", payment);
            model.addAttribute("methods", providerFactory.getAvailableMethods());
            model.addAttribute("cryptoNetworks", getCryptoNetworks());
            model.addAttribute("rates", exchangeRateService.getRates());
            return "payment/choose-method";
        }

        model.addAttribute("payment", payment);
        if (payment.getRedirectUrl() != null && !payment.getRedirectUrl().isBlank()) {
            model.addAttribute("resolvedRedirectUrl", resolveRedirectUrl(payment));
        }
        return "payment/status";
    }

    private String resolveRedirectUrl(Payment payment) {
        return payment.getRedirectUrl()
                .replace("{payment_id}", String.valueOf(payment.getId()))
                .replace("{order_id}", payment.getOrderId() != null ? payment.getOrderId() : "")
                .replace("{status}", payment.getStatus().name())
                .replace("{amount}", payment.getAmount().toPlainString())
                .replace("{currency}", payment.getCurrency());
    }

    /**
     * Подтверждает выбранный метод оплаты для платежа.
     *
     * @param id      идентификатор платежа
     * @param request данные подтверждения
     * @return ответ с информацией о платеже или ошибкой
     */
    @PostMapping("/pay/{id}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmPayment(
            @PathVariable UUID id,
            @RequestBody ConfirmPaymentRequest request
    ) {
        try {
            var payment = paymentService.confirmPayment(id, request);
            return ResponseEntity.ok(PaymentResponse.from(payment, appProperties));
        } catch (PaymentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder().message(e.getMessage()).build());
        }
    }

    /**
     * Сбрасывает выбранный метод оплаты.
     *
     * @param id идентификатор платежа
     * @return пустой ответ 200
     */
    @PostMapping("/pay/{id}/reset")
    @ResponseBody
    public ResponseEntity<Void> resetMethod(@PathVariable UUID id) {
        paymentService.resetPaymentMethod(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Проксирует запрос подсказок организаций через DaData API.
     *
     * @param body тело запроса
     * @return ответ от DaData
     */
    @PostMapping("/api/v1/suggest/party")
    @ResponseBody
    public ResponseEntity<String> suggestParty(@RequestBody String body) {
        var dadataKey = env.getProperty("dadata.api-key", "");
        if (dadataKey.isBlank()) {
            return ResponseEntity.ok("{}");
        }
        try {
            var result = RestClient.create().post()
                    .uri("https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/party")
                    .header("Authorization", "Token " + dadataKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok("{}");
        }
    }

    /**
     * Возвращает текущие курсы криптовалют.
     *
     * @return карта сеть-курс
     */
    @GetMapping("/api/v1/rates")
    @ResponseBody
    public Map<CryptoNetwork, BigDecimal> getRates() {
        return exchangeRateService.getRates();
    }

    private Map<String, String> getCryptoNetworks() {
        return cryptoProvider.isEnabled()
                ? cryptoProvider.getEnabledNetworks()
                : Map.of();
    }
}
