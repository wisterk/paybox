package me.wisterk.paybox.service;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import me.wisterk.paybox.domain.enums.PaymentStatus;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис кэширования статусов платежей в Redis.
 */
@Service
@RequiredArgsConstructor
@ToString
public class PaymentStatusService {

    /** Префикс ключа в Redis. */
    private static final String KEY_PREFIX = "payment:status:";

    /** Время жизни записи в кэше. */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** Шаблон для работы с Redis. */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Сохраняет статус платежа в кэш.
     *
     * @param paymentId идентификатор платежа
     * @param status    статус платежа
     */
    public void cacheStatus(UUID paymentId, PaymentStatus status) {
        redisTemplate.opsForValue().set(KEY_PREFIX + paymentId, status.name(), CACHE_TTL);
    }

    /**
     * Возвращает закэшированный статус платежа.
     *
     * @param paymentId идентификатор платежа
     * @return статус, если найден в кэше
     */
    public Optional<PaymentStatus> getCachedStatus(UUID paymentId) {
        var val = redisTemplate.opsForValue().get(KEY_PREFIX + paymentId);
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(PaymentStatus.valueOf(val));
    }
}
