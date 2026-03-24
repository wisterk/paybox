package ru.sterkhov.onlinepaymentsystem.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.sterkhov.onlinepaymentsystem.service.DonationAlertsOAuthService;

/**
 * Контроллер обработки OAuth-колбэков от DonationAlerts.
 */
@Controller
@RequiredArgsConstructor
@ToString
public class OAuthCallbackController {

    /** Сервис OAuth-авторизации DonationAlerts. */
    private final DonationAlertsOAuthService daOAuthService;

    /**
     * Обрабатывает OAuth-колбэк от DonationAlerts.
     *
     * @param code  код авторизации
     * @param model модель для шаблона
     * @return имя шаблона
     */
    @GetMapping("/oauth/donationalerts/callback")
    public String donationAlertsCallback(@RequestParam("code") String code, Model model) {
        var token = daOAuthService.exchangeCode(code);
        if (token != null) {
            model.addAttribute("errorTitle", "DonationAlerts подключён");
            model.addAttribute("errorMessage", "OAuth-токен получен и сохранён. Интеграция активна.");
        } else {
            model.addAttribute("errorTitle", "Ошибка авторизации");
            model.addAttribute("errorMessage", "Не удалось получить токен DonationAlerts.");
        }
        return "error/payment-error";
    }

    /**
     * Перенаправляет на страницу авторизации DonationAlerts.
     *
     * @return URL редиректа
     */
    @GetMapping("/oauth/donationalerts/connect")
    public String connectDonationAlerts() {
        return "redirect:" + daOAuthService.getAuthorizationUrl();
    }
}
