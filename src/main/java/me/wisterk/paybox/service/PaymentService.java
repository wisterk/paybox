package me.wisterk.paybox.service;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.wisterk.paybox.domain.entity.Payment;
import me.wisterk.paybox.domain.enums.PaymentStatus;
import me.wisterk.paybox.dto.payment.ConfirmPaymentRequest;
import me.wisterk.paybox.dto.payment.CreatePaymentRequest;
import me.wisterk.paybox.exception.PaymentException;
import me.wisterk.paybox.repository.PaymentRepository;
import me.wisterk.paybox.service.provider.PaymentProviderFactory;
import me.wisterk.paybox.websocket.PaymentStatusNotifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Основной сервис для управления жизненным циклом платежей.
 */
@Service
@RequiredArgsConstructor
@ToString
@Slf4j
public class PaymentService {

    /** Репозиторий платежей. */
    private final PaymentRepository paymentRepository;

    /** Фабрика провайдеров. */
    private final PaymentProviderFactory providerFactory;

    /** Нотификатор статусов по WebSocket. */
    private final PaymentStatusNotifier statusNotifier;

    /** Сервис кэширования статусов. */
    private final PaymentStatusService paymentStatusService;

    /** Время жизни платежа в минутах. */
    @Value("${payment.payment-ttl-minutes:30}")
    private int paymentTtlMinutes;

    /**
     * Создаёт новый платёж.
     *
     * @param request данные для создания платежа
     * @return созданный платёж
     */
    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        var payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "RUB");
        payment.setOrderId(request.getOrderId());
        payment.setRedirectUrl(request.getRedirectUrl());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setExpiresAt(Instant.now().plus(Duration.ofMinutes(paymentTtlMinutes)));

        if (request.getMethod() != null) {
            var provider = providerFactory.getProvider(request.getMethod());
            payment.setMethod(request.getMethod());
            payment.setProviderName(provider.getProviderName());
            payment = paymentRepository.save(payment);
            payment = provider.initiate(payment, request);
        } else {
            payment.setProviderName("pending");
            payment = paymentRepository.save(payment);
        }

        payment = paymentRepository.save(payment);
        paymentStatusService.cacheStatus(payment.getId(), payment.getStatus());

        log.info("Payment created: id={}, method={}, amount={} {}",
                payment.getId(), payment.getMethod(), payment.getAmount(), payment.getCurrency());
        return payment;
    }

    /**
     * Подтверждает выбор метода оплаты для существующего платежа.
     *
     * @param id      идентификатор платежа
     * @param confirm данные подтверждения
     * @return обновлённый платёж
     * @throws PaymentException если метод уже выбран или статус не позволяет подтверждение
     */
    @Transactional
    public Payment confirmPayment(UUID id, ConfirmPaymentRequest confirm) {
        var payment = getPayment(id);

        if (payment.getMethod() != null) {
            throw new PaymentException("Payment method already set");
        }
        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new PaymentException("Payment cannot be confirmed in status: " + payment.getStatus());
        }

        var method = confirm.getMethod();
        var provider = providerFactory.getProvider(method);
        payment.setMethod(method);
        payment.setProviderName(provider.getProviderName());

        var request = CreatePaymentRequest.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(method)
                .orderId(payment.getOrderId())
                .cryptoNetwork(confirm.getCryptoNetwork())
                .payer(confirm.getPayer())
                .contactEmails(confirm.getContactEmails())
                .build();

        payment = provider.initiate(payment, request);
        payment = paymentRepository.save(payment);
        paymentStatusService.cacheStatus(payment.getId(), payment.getStatus());

        log.info("Payment {} confirmed: method={}", id, method);
        return payment;
    }

    /**
     * Находит все платежи по идентификатору заказа.
     *
     * @param orderId идентификатор заказа
     * @return список платежей
     */
    @Transactional(readOnly = true)
    public List<Payment> findByOrderId(String orderId) {
        return paymentRepository.findAllByOrderId(orderId);
    }

    /**
     * Возвращает платёж по идентификатору.
     *
     * @param id идентификатор платежа
     * @return платёж
     * @throws PaymentException если платёж не найден
     */
    @Transactional(readOnly = true)
    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found: " + id));
    }

    /**
     * Ищет платёж по идентификатору.
     *
     * @param id идентификатор платежа
     * @return платёж, если найден
     */
    @Transactional(readOnly = true)
    public Optional<Payment> findPayment(UUID id) {
        return paymentRepository.findById(id);
    }

    /**
     * Сбрасывает выбранный метод оплаты.
     *
     * @param id идентификатор платежа
     * @throws PaymentException если платёж в терминальном состоянии
     */
    @Transactional
    public void resetPaymentMethod(UUID id) {
        var payment = getPayment(id);
        if (payment.getStatus().isTerminal()) {
            throw new PaymentException("Cannot reset terminal payment");
        }
        if (payment.getMetadata() != null && payment.getMetadata().containsKey("originalAmount")) {
            var originalCurrency = (String) payment.getMetadata().get("originalCurrency");
            if (originalCurrency != null) {
                payment.setAmount(new java.math.BigDecimal((String) payment.getMetadata().get("originalAmount")));
                payment.setCurrency(originalCurrency);
            }
        }
        payment.setMethod(null);
        payment.setProviderName(null);
        payment.setExternalId(null);
        payment.setPaymentUrl(null);
        payment.setMetadata(null);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setExpiresAt(null);
        paymentRepository.save(payment);
        log.info("Payment {} method reset", id);
    }

    /**
     * Обновляет статус платежа из провайдера.
     *
     * @param id идентификатор платежа
     * @return обновлённый платёж
     */
    @Transactional
    public Payment refreshStatus(UUID id) {
        var payment = getPayment(id);
        if (payment.getStatus().isTerminal() || payment.getMethod() == null) {
            return payment;
        }

        var provider = providerFactory.getProvider(payment.getMethod());
        var newStatus = provider.checkStatus(payment);

        if (newStatus != payment.getStatus()) {
            return updateStatus(payment, newStatus);
        }
        return payment;
    }

    /**
     * Обновляет статус платежа и отправляет уведомление.
     *
     * @param payment   платёж
     * @param newStatus новый статус
     * @return обновлённый платёж
     */
    @Transactional
    public Payment updateStatus(Payment payment, PaymentStatus newStatus) {
        var oldStatus = payment.getStatus();
        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.PAID) {
            payment.setPaidAt(Instant.now());
        }
        payment = paymentRepository.save(payment);
        paymentStatusService.cacheStatus(payment.getId(), newStatus);

        log.info("Payment {} status: {} -> {}", payment.getId(), oldStatus, newStatus);
        statusNotifier.notify(payment);
        return payment;
    }

    /**
     * Находит все платежи в незавершённых статусах.
     *
     * @return список ожидающих платежей
     */
    @Transactional(readOnly = true)
    public List<Payment> findPendingPayments() {
        return paymentRepository.findByStatusIn(
                List.of(PaymentStatus.CREATED, PaymentStatus.PENDING));
    }

    /**
     * Обновляет статус платежа по внешнему идентификатору.
     *
     * @param externalId внешний идентификатор провайдера
     * @param newStatus  новый статус
     * @return обновлённый платёж
     * @throws PaymentException если платёж не найден
     */
    @Transactional
    public Payment updateStatusByExternalId(String externalId, PaymentStatus newStatus) {
        var payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new PaymentException("Payment not found for externalId: " + externalId));

        if (payment.getStatus().isTerminal()) {
            log.debug("Ignoring status update for terminal payment {}", payment.getId());
            return payment;
        }
        return updateStatus(payment, newStatus);
    }
}
