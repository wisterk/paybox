package ru.sterkhov.onlinepaymentsystem.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sterkhov.onlinepaymentsystem.config.DonationProperties;
import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;
import ru.sterkhov.onlinepaymentsystem.dto.payment.CreatePaymentRequest;
import ru.sterkhov.onlinepaymentsystem.service.DonationAlertsOAuthService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Провайдер оплаты через DonationAlerts.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class DonationAlertsProvider implements PaymentProvider {

    /** Настройки провайдеров донатов. */
    private final DonationProperties props;

    /** OAuth-сервис DonationAlerts. */
    private final DonationAlertsOAuthService oauthService;

    /** HTTP-клиент для запросов к DonationAlerts API. */
    private final RestClient httpClient = RestClient.builder()
            .defaultHeader("Accept", "application/json")
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.DONATION_ALERTS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return "donation-alerts";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        var da = props.getDonationAlerts();
        return da.isEnabled() && da.getPageName() != null && !da.getPageName().isBlank();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment initiate(Payment payment, CreatePaymentRequest request) {
        var da = props.getDonationAlerts();
        var refCode = "PB-" + payment.getId().toString().substring(0, 8).toUpperCase();
        var donateUrl = "https://www.donationalerts.com/r/" + da.getPageName();

        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderName(getProviderName());
        payment.setExternalId(refCode);
        payment.setPaymentUrl(donateUrl);

        var meta = new LinkedHashMap<String, Object>();
        meta.put("refCode", refCode);
        meta.put("donateUrl", donateUrl);
        meta.put("serviceName", "DonationAlerts");
        payment.setMetadata(meta);

        log.info("DonationAlerts payment created: refCode={}, url={}", refCode, donateUrl);
        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PaymentStatus checkStatus(Payment payment) {
        var accessToken = oauthService.getAccessToken();
        if (accessToken == null) {
            return PaymentStatus.PENDING;
        }

        var refCode = payment.getExternalId();
        try {
            var response = httpClient.get()
                    .uri("https://www.donationalerts.com/api/v1/alerts/donations?page=1")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return PaymentStatus.PENDING;

            var data = (List<Map<String, Object>>) response.get("data");
            if (data == null) return PaymentStatus.PENDING;

            for (var donation : data) {
                var message = String.valueOf(donation.get("message"));
                if (message.contains(refCode)) {
                    var donatedAmount = new BigDecimal(String.valueOf(donation.get("amount")));
                    if (donatedAmount.compareTo(payment.getAmount()) >= 0) {
                        log.info("DonationAlerts payment matched: refCode={}, amount={}", refCode, donatedAmount);
                        return PaymentStatus.PAID;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check DonationAlerts: {}", e.getMessage());
        }
        return PaymentStatus.PENDING;
    }
}
