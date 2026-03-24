package me.wisterk.paybox.service.provider;

import lombok.ToString;
import org.springframework.stereotype.Component;
import me.wisterk.paybox.domain.enums.PaymentMethod;
import me.wisterk.paybox.exception.ProviderNotAvailableException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Фабрика для получения доступных платёжных провайдеров.
 */
@Component
@ToString
public class PaymentProviderFactory {

    /** Карта доступных провайдеров по методам оплаты. */
    private final Map<PaymentMethod, PaymentProvider> providers;

    /**
     * Создаёт фабрику, фильтруя только включённые провайдеры.
     *
     * @param allProviders список всех зарегистрированных провайдеров
     */
    public PaymentProviderFactory(List<PaymentProvider> allProviders) {
        this.providers = allProviders.stream()
                .filter(PaymentProvider::isEnabled)
                .collect(Collectors.toMap(PaymentProvider::getMethod, Function.identity()));
    }

    /**
     * Возвращает провайдер для указанного метода оплаты.
     *
     * @param method метод оплаты
     * @return провайдер
     * @throws ProviderNotAvailableException если провайдер недоступен
     */
    public PaymentProvider getProvider(PaymentMethod method) {
        var provider = providers.get(method);
        if (provider == null) {
            throw new ProviderNotAvailableException(
                    "Payment method " + method + " is not available");
        }
        return provider;
    }

    /**
     * Возвращает набор доступных методов оплаты.
     *
     * @return неизменяемый набор методов
     */
    public Set<PaymentMethod> getAvailableMethods() {
        return Collections.unmodifiableSet(providers.keySet());
    }
}
