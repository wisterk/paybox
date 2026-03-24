package ru.sterkhov.onlinepaymentsystem.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.sterkhov.onlinepaymentsystem.config.TBankProperties;
import ru.sterkhov.onlinepaymentsystem.domain.entity.Payment;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentMethod;
import ru.sterkhov.onlinepaymentsystem.domain.enums.PaymentStatus;
import ru.sterkhov.onlinepaymentsystem.dto.payment.CreatePaymentRequest;
import ru.sterkhov.onlinepaymentsystem.dto.tbank.TBankInvoiceRequest;
import ru.sterkhov.onlinepaymentsystem.dto.tbank.TBankInvoiceResponse;
import ru.sterkhov.onlinepaymentsystem.dto.tbank.TBankInvoiceStatusResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Провайдер выставления счетов через API Т-Банка.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class TBankInvoiceProvider implements PaymentProvider {

    /** Счётчик номеров счетов. */
    private static final AtomicLong INVOICE_COUNTER = new AtomicLong(System.currentTimeMillis() % 1_000_000_000);

    /** HTTP-клиент Т-Банка. */
    private final TBankBaseClient tbankClient;

    /** Настройки Т-Банка. */
    private final TBankProperties tbankProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.INVOICE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return "tbank-invoice";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return tbankProperties.isEnabled() && tbankProperties.getInvoice().isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment initiate(Payment payment, CreatePaymentRequest request) {
        var invoiceRequest = buildInvoiceRequest(payment, request);
        var response = tbankClient.post("/v1/invoice/send", invoiceRequest, TBankInvoiceResponse.class);

        payment.setExternalId(response.invoiceId());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderName(getProviderName());

        var meta = new LinkedHashMap<String, Object>();
        if (request.getPayer() != null) {
            if (request.getPayer().getName() != null) meta.put("payerName", request.getPayer().getName());
            if (request.getPayer().getInn() != null) meta.put("payerInn", request.getPayer().getInn());
        }
        if (request.getContactEmails() != null && !request.getContactEmails().isEmpty()) {
            meta.put("contactEmail", request.getContactEmails().get(0));
        }
        if (!meta.isEmpty()) {
            payment.setMetadata(meta);
        }

        log.info("Invoice created: externalId={} for payment={}", response.invoiceId(), payment.getId());
        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentStatus checkStatus(Payment payment) {
        var response = tbankClient.get(
                "/v1/openapi/invoice/" + payment.getExternalId() + "/info",
                TBankInvoiceStatusResponse.class);

        log.debug("Invoice {} status from T-Bank: {}", payment.getExternalId(), response.status());
        return mapStatus(response.status());
    }

    private PaymentStatus mapStatus(String tbankStatus) {
        if (tbankStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (tbankStatus.toUpperCase()) {
            case "NEW", "SUBMITTED", "VIEWED", "НОВЫЙ", "ОТПРАВЛЕН", "ПРОСМОТРЕН" -> PaymentStatus.PENDING;
            case "PAID", "EXECUTED", "ОПЛАЧЕН", "ИСПОЛНЕН" -> PaymentStatus.PAID;
            case "CANCELLED", "ОТМЕНЕН" -> PaymentStatus.CANCELLED;
            case "EXPIRED", "ПРОСРОЧЕН" -> PaymentStatus.EXPIRED;
            default -> {
                log.warn("Unknown T-Bank invoice status: {}", tbankStatus);
                yield PaymentStatus.PENDING;
            }
        };
    }

    private TBankInvoiceRequest buildInvoiceRequest(Payment payment, CreatePaymentRequest request) {
        var invoiceNumber = String.valueOf(INVOICE_COUNTER.incrementAndGet());
        var dueDate = LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);

        TBankInvoiceRequest.Payer payer = null;
        if (request.getPayer() != null) {
            payer = TBankInvoiceRequest.Payer.builder()
                    .name(request.getPayer().getName())
                    .inn(request.getPayer().getInn())
                    .kpp(request.getPayer().getKpp())
                    .build();
        }

        List<TBankInvoiceRequest.Item> items;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            items = request.getItems().stream()
                    .map(i -> TBankInvoiceRequest.Item.builder()
                            .name(i.getName())
                            .price(i.getPrice())
                            .unit(i.getUnit())
                            .vat(i.getVat())
                            .amount(i.getAmount())
                            .build())
                    .toList();
        } else {
            items = List.of(TBankInvoiceRequest.Item.builder()
                    .name(request.getDescription() != null ? request.getDescription() : "Оплата")
                    .price(payment.getAmount().doubleValue())
                    .unit("шт")
                    .vat("None")
                    .amount(1)
                    .build());
        }

        var contacts = new ArrayList<TBankInvoiceRequest.Contact>();
        if (request.getContactEmails() != null) {
            request.getContactEmails().forEach(email ->
                    contacts.add(TBankInvoiceRequest.Contact.builder().email(email).build()));
        }
        if (request.getContactPhones() != null) {
            request.getContactPhones().forEach(phone ->
                    contacts.add(TBankInvoiceRequest.Contact.builder().phone(phone).build()));
        }

        return TBankInvoiceRequest.builder()
                .invoiceNumber(invoiceNumber)
                .dueDate(dueDate)
                .accountNumber(tbankProperties.getAccountNumber())
                .payer(payer)
                .items(items)
                .contacts(contacts.isEmpty() ? null : contacts)
                .comment(request.getComment())
                .customPaymentPurpose("Оплата по счёту " + invoiceNumber)
                .build();
    }
}
