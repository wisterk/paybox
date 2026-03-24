package me.wisterk.paybox.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import me.wisterk.paybox.dto.tbank.TBankErrorResponse;
import me.wisterk.paybox.exception.PaymentException;

/**
 * Базовый HTTP-клиент для взаимодействия с API Т-Банка.
 */
@Component
@RequiredArgsConstructor
@ToString
@Slf4j
public class TBankBaseClient {

    /** REST-клиент для Т-Банка. */
    private final RestClient tbankRestClient;

    /** Маппер для сериализации JSON. */
    private final ObjectMapper objectMapper;

    /**
     * Выполняет POST-запрос к API Т-Банка.
     *
     * @param path         путь эндпоинта
     * @param body         тело запроса
     * @param responseType тип ответа
     * @param <T>          тип ответа
     * @return десериализованный ответ
     */
    public <T> T post(String path, Object body, Class<T> responseType) {
        return tbankRestClient.post()
                .uri(path)
                .body(body)
                .exchange((request, response) -> handleResponse(path, response, responseType));
    }

    /**
     * Выполняет GET-запрос к API Т-Банка.
     *
     * @param path         путь эндпоинта
     * @param responseType тип ответа
     * @param <T>          тип ответа
     * @return десериализованный ответ
     */
    public <T> T get(String path, Class<T> responseType) {
        return tbankRestClient.get()
                .uri(path)
                .exchange((request, response) -> handleResponse(path, response, responseType));
    }

    private <T> T handleResponse(
            String path,
            RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
            Class<T> responseType
    ) throws java.io.IOException {
        var status = response.getStatusCode();
        if (status.is2xxSuccessful()) {
            return response.bodyTo(responseType);
        }

        var rawBody = new String(response.getBody().readAllBytes());
        log.error("T-Bank API error on {}: status={}, body={}", path, status.value(), rawBody);

        try {
            var error = objectMapper.readValue(rawBody, TBankErrorResponse.class);
            throw new PaymentException(
                    "T-Bank API error [%s]: %s".formatted(error.errorCode(), error.errorMessage()));
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException(
                    "T-Bank API error: HTTP %d — %s".formatted(status.value(), rawBody));
        }
    }
}
