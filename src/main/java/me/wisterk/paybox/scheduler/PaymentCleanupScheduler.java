package me.wisterk.paybox.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import me.wisterk.paybox.repository.PaymentRepository;

import java.time.Duration;
import java.time.Instant;

/**
 * Планировщик очистки просроченных и отменённых платежей.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class PaymentCleanupScheduler {

    /** Репозиторий платежей. */
    private final PaymentRepository paymentRepository;

    /** Количество дней, после которых платёж удаляется. */
    @Value("${payment.cleanup.after-days:7}")
    private int cleanupAfterDays;

    /**
     * Удаляет просроченные и отменённые платежи по расписанию.
     */
    @Scheduled(cron = "${payment.cleanup.cron:0 0 3 * * *}")
    public void cleanupExpiredPayments() {
        var cutoff = Instant.now().minus(Duration.ofDays(cleanupAfterDays));
        var deleted = paymentRepository.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired/cancelled payments older than {} days", deleted, cleanupAfterDays);
        }
    }
}
