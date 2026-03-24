package ru.sterkhov.onlinepaymentsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация безопасности приложения (Spring Security).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** API-ключ для аутентификации запросов к REST API. */
    @Value("${payment.api-key}")
    private String apiKey;

    /**
     * Цепочка фильтров для защиты API-эндпоинтов платежей.
     *
     * @param http построитель конфигурации безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/payments/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ApiKeyAuthFilter(apiKey), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    /**
     * Цепочка фильтров для вебхуков (публичный доступ).
     *
     * @param http построитель конфигурации безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webhookSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/webhooks/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Цепочка фильтров для публичных эндпоинтов (UI, статика, Swagger).
     *
     * @param http построитель конфигурации безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    @Order(3)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/", "/pay/**", "/sandbox/**", "/api/v1/rates", "/api/v1/suggest/**",
                        "/oauth/**", "/ws/**", "/css/**", "/js/**",
                        "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html"
                )
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Цепочка фильтров по умолчанию (разрешает все запросы).
     *
     * @param http построитель конфигурации безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    @Order(4)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
