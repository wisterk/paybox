package me.wisterk.paybox.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import me.wisterk.paybox.config.AppProperties;
import me.wisterk.paybox.service.provider.PaymentProviderFactory;

/**
 * Контроллер песочницы для тестирования платежей.
 */
@Controller
@RequiredArgsConstructor
@ToString
@ConditionalOnProperty(name = "app.sandbox.enabled", havingValue = "true", matchIfMissing = true)
public class SandboxController {

    /** Настройки приложения. */
    private final AppProperties appProperties;

    /** Фабрика провайдеров. */
    private final PaymentProviderFactory providerFactory;

    /**
     * Отображает страницу песочницы.
     *
     * @param model модель для шаблона
     * @return имя шаблона
     */
    @GetMapping("/sandbox")
    public String sandbox(Model model) {
        model.addAttribute("baseUrl", appProperties.getBaseUrl());
        model.addAttribute("methods", providerFactory.getAvailableMethods());
        return "sandbox/index";
    }
}
