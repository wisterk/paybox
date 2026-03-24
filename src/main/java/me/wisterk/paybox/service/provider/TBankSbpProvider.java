package me.wisterk.paybox.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import me.wisterk.paybox.config.TBankProperties;
import me.wisterk.paybox.domain.entity.Payment;
import me.wisterk.paybox.domain.enums.PaymentMethod;
import me.wisterk.paybox.domain.enums.PaymentStatus;
import me.wisterk.paybox.dto.payment.CreatePaymentRequest;
import me.wisterk.paybox.dto.tbank.TBankSbpQrRequest;
import me.wisterk.paybox.dto.tbank.TBankSbpQrResponse;
import me.wisterk.paybox.dto.tbank.TBankSbpQrStatusResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Провайдер оплаты через СБП QR-код (Т-Банк).
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class TBankSbpProvider implements PaymentProvider {

    /** Время жизни QR-кода по умолчанию (минуты). */
    private static final int DEFAULT_TTL_MINUTES = 15;

    /** HTTP-клиент Т-Банка. */
    private final TBankBaseClient tbankClient;

    /** Настройки Т-Банка. */
    private final TBankProperties tbankProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.SBP_QR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return "tbank-sbp";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return tbankProperties.isEnabled() && tbankProperties.getSbp().isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment initiate(Payment payment, CreatePaymentRequest request) {
        var qrRequest = TBankSbpQrRequest.builder()
                .accountNumber(tbankProperties.getAccountNumber())
                .sum(payment.getAmount().doubleValue())
                .purpose(request.getDescription() != null
                        ? request.getDescription()
                        : "Оплата заказа " + payment.getOrderId())
                .ttl(DEFAULT_TTL_MINUTES)
                .redirectUrl(request.getRedirectUrl())
                .build();

        var response = tbankClient.post("/v1/b2b/qr/onetime", qrRequest, TBankSbpQrResponse.class);

        payment.setExternalId(response.qrId());
        payment.setPaymentUrl(response.paymentUrl());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderName(getProviderName());
        payment.setMetadata(Map.of("qrData", response.data() != null ? response.data() : ""));
        payment.setExpiresAt(Instant.now().plus(Duration.ofMinutes(DEFAULT_TTL_MINUTES)));

        log.info("SBP QR created: qrId={} for payment={}", response.qrId(), payment.getId());
        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentStatus checkStatus(Payment payment) {
        var response = tbankClient.get(
                "/v1/b2b-qr/" + payment.getExternalId() + "/info",
                TBankSbpQrStatusResponse.class);

        log.debug("SBP QR {} status from T-Bank: {}", payment.getExternalId(), response.status());
        return mapStatus(response.status());
    }

    private PaymentStatus mapStatus(String sbpStatus) {
        if (sbpStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (sbpStatus.toUpperCase()) {
            case "CREATED", "PROCESSING" -> PaymentStatus.PENDING;
            case "PAID", "ACCEPTED" -> PaymentStatus.PAID;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            case "REJECTED" -> PaymentStatus.FAILED;
            default -> {
                log.warn("Unknown SBP QR status: {}", sbpStatus);
                yield PaymentStatus.PENDING;
            }
        };
    }
}
