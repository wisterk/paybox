package me.wisterk.paybox.service.provider;

import me.wisterk.paybox.domain.entity.Payment;
import me.wisterk.paybox.domain.enums.PaymentMethod;
import me.wisterk.paybox.domain.enums.PaymentStatus;
import me.wisterk.paybox.dto.payment.CreatePaymentRequest;

/**
 * Интерфейс платёжного провайдера.
 */
public interface PaymentProvider {

    /**
     * Возвращает метод оплаты, обслуживаемый провайдером.
     *
     * @return метод оплаты
     */
    PaymentMethod getMethod();

    /**
     * Возвращает имя провайдера.
     *
     * @return строковое имя провайдера
     */
    String getProviderName();

    /**
     * Проверяет, доступен ли провайдер.
     *
     * @return {@code true}, если провайдер включён и настроен
     */
    boolean isEnabled();

    /**
     * Инициирует платёж через провайдера.
     *
     * @param payment платёж для инициации
     * @param request данные запроса на создание
     * @return обновлённый платёж
     */
    Payment initiate(Payment payment, CreatePaymentRequest request);

    /**
     * Проверяет текущий статус платежа у провайдера.
     *
     * @param payment платёж для проверки
     * @return текущий статус платежа
     */
    PaymentStatus checkStatus(Payment payment);
}
