package me.wisterk.paybox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import me.wisterk.paybox.domain.entity.Payment;
import me.wisterk.paybox.domain.enums.PaymentStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с сущностями платежей.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Находит платежи по набору статусов.
     *
     * @param statuses набор статусов
     * @return список платежей
     */
    List<Payment> findByStatusIn(Collection<PaymentStatus> statuses);

    /**
     * Находит платёж по внешнему идентификатору провайдера.
     *
     * @param externalId внешний идентификатор
     * @return платёж, если найден
     */
    Optional<Payment> findByExternalId(String externalId);

    /**
     * Находит платёж по идентификатору заказа.
     *
     * @param orderId идентификатор заказа
     * @return платёж, если найден
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Находит все платежи по идентификатору заказа.
     *
     * @param orderId идентификатор заказа
     * @return список платежей
     */
    List<Payment> findAllByOrderId(String orderId);

    /**
     * Удаляет просроченные и отменённые платежи старше указанной даты.
     *
     * @param cutoff пороговая дата
     * @return количество удалённых записей
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Payment p WHERE p.status IN ('EXPIRED', 'CANCELLED') AND p.updatedAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
