package ru.sterkhov.onlinepaymentsystem.websocket;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.sterkhov.onlinepaymentsystem.config.AppProperties;
import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.dto.payment.PaymentResponse;

/**
 * Компонент для отправки уведомлений о статусе платежа по WebSocket.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class PaymentStatusNotifier {

    /** Шаблон для отправки STOMP-сообщений. */
    private final SimpMessagingTemplate messagingTemplate;

    /** Настройки приложения. */
    private final AppProperties appProperties;

    /**
     * Отправляет уведомление об обновлении статуса платежа.
     *
     * @param payment платёж с обновлённым статусом
     */
    public void notify(Payment payment) {
        var response = PaymentResponse.from(payment, appProperties);
        var destination = "/topic/payments/" + payment.getId();
        messagingTemplate.convertAndSend(destination, response);
        log.debug("WebSocket notification sent to {} — status: {}", destination, payment.getStatus());
    }
}
