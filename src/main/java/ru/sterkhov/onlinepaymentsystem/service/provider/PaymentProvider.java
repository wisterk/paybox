package ru.sterkhov.onlinepaymentsystem.service.provider;

import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;
import ru.sterkhov.onlinepaymentsystem.dto.payment.CreatePaymentRequest;

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
