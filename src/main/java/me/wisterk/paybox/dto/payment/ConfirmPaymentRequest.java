package me.wisterk.paybox.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.wisterk.paybox.domain.enums.PaymentMethod;

import java.util.List;

/**
 * Запрос на подтверждение способа оплаты для уже созданного платежа.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {

    /** Выбранный метод оплаты. */
    @NotNull
    private PaymentMethod method;

    /** Криптовалютная сеть (для крипто-платежей). */
    private String cryptoNetwork;

    /** Данные плательщика (для счетов). */
    private CreatePaymentRequest.InvoicePayer payer;

    /** Контактные email-адреса. */
    private List<String> contactEmails;
}
