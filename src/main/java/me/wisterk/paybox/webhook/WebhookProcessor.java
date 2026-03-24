package me.wisterk.paybox.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import me.wisterk.paybox.domain.enums.PaymentStatus;
import me.wisterk.paybox.service.PaymentService;

/**
 * Обработчик входящих вебхуков от Т-Банка.
 */
@Service
@RequiredArgsConstructor
@ToString
@Slf4j
public class WebhookProcessor {

    /** Сервис платежей. */
    private final PaymentService paymentService;

    /** Маппер JSON. */
    private final ObjectMapper objectMapper;

    /**
     * Обрабатывает тело вебхука, определяя его тип и обновляя статус платежа.
     *
     * @param rawBody сырое тело вебхука
     */
    public void process(String rawBody) {
        try {
            var root = objectMapper.readTree(rawBody);
            log.info("Processing T-Bank webhook: {}", rawBody);

            if (root.has("invoiceId")) {
                processInvoiceWebhook(root);
            } else if (root.has("qrId")) {
                processSbpWebhook(root);
            } else {
                log.warn("Unknown webhook type: {}", rawBody);
            }
        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage(), e);
        }
    }

    private void processInvoiceWebhook(JsonNode root) {
        var invoiceId = root.get("invoiceId").asText();
        var status = root.has("status") ? root.get("status").asText() : null;

        var paymentStatus = mapInvoiceStatus(status);
        if (paymentStatus != null) {
            paymentService.updateStatusByExternalId(invoiceId, paymentStatus);
            log.info("Invoice webhook processed: invoiceId={}, status={}", invoiceId, paymentStatus);
        }
    }

    private void processSbpWebhook(JsonNode root) {
        var qrId = root.get("qrId").asText();
        var status = root.has("status") ? root.get("status").asText() : null;

        var paymentStatus = mapSbpStatus(status);
        if (paymentStatus != null) {
            paymentService.updateStatusByExternalId(qrId, paymentStatus);
            log.info("SBP webhook processed: qrId={}, status={}", qrId, paymentStatus);
        }
    }

    private PaymentStatus mapInvoiceStatus(String status) {
        if (status == null) return null;
        return switch (status) {
            case "Оплачен" -> PaymentStatus.PAID;
            case "Отменен" -> PaymentStatus.CANCELLED;
            case "Просрочен" -> PaymentStatus.EXPIRED;
            default -> null;
        };
    }

    private PaymentStatus mapSbpStatus(String status) {
        if (status == null) return null;
        return switch (status.toUpperCase()) {
            case "PAID", "ACCEPTED" -> PaymentStatus.PAID;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            case "REJECTED" -> PaymentStatus.FAILED;
            default -> null;
        };
    }
}
