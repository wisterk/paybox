package ru.sterkhov.onlinepaymentsystem.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;
import ru.sterkhov.onlinepaymentsystem.service.PaymentService;

import java.time.Instant;

/**
 * Планировщик опроса статусов ожидающих платежей.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class PaymentStatusPollingScheduler {

    /** Сервис платежей. */
    private final PaymentService paymentService;

    /**
     * Опрашивает провайдеров для обновления статусов ожидающих платежей.
     */
    @Scheduled(fixedDelayString = "${payment.polling.interval:30000}")
    public void pollPendingPayments() {
        var pending = paymentService.findPendingPayments();
        if (pending.isEmpty()) return;

        log.debug("Polling {} pending payments", pending.size());
        for (var payment : pending) {
            try {
                if (payment.getExpiresAt() != null && Instant.now().isAfter(payment.getExpiresAt())) {
                    paymentService.updateStatus(payment, PaymentStatus.EXPIRED);
                    continue;
                }
                paymentService.refreshStatus(payment.getId());
            } catch (Exception e) {
                log.warn("Failed to poll payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}
