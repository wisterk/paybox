package ru.sterkhov.onlinepaymentsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Конфигурация подключения к Redis.
 */
@Configuration
public class RedisConfig {

    /**
     * Создаёт {@link RedisTemplate} со строковой сериализацией ключей и значений.
     *
     * @param factory фабрика подключений к Redis
     * @return настроенный шаблон для работы с Redis
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        var template = new RedisTemplate<String, String>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
