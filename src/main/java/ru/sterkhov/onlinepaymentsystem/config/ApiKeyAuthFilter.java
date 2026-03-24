package ru.sterkhov.onlinepaymentsystem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Фильтр аутентификации по API-ключу в заголовке {@code X-API-Key}.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    /** Имя HTTP-заголовка с API-ключом. */
    private static final String API_KEY_HEADER = "X-API-Key";

    /** Ожидаемое значение API-ключа. */
    private final String expectedApiKey;

    /**
     * Создаёт фильтр с указанным ожидаемым API-ключом.
     *
     * @param expectedApiKey ожидаемое значение API-ключа
     */
    public ApiKeyAuthFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var apiKey = request.getHeader(API_KEY_HEADER);

        if (expectedApiKey.equals(apiKey)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "api-client", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
