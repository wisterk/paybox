package me.wisterk.paybox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация HTTP-клиента для взаимодействия с API Т-Банка.
 */
@Configuration
@Slf4j
public class TBankClientConfig {

    /**
     * Создаёт экземпляр {@link ObjectMapper} для сериализации/десериализации JSON.
     *
     * @return настроенный {@link ObjectMapper}
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Создаёт {@link RestClient} для запросов к API Т-Банка.
     *
     * @param props   настройки подключения к Т-Банку
     * @param builder построитель REST-клиента
     * @return настроенный REST-клиент
     */
    @Bean
    public RestClient tbankRestClient(TBankProperties props, RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(props.getConnectTimeout());
        requestFactory.setReadTimeout(props.getReadTimeout());

        return builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getBearerToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("T-Bank API: {} {}", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }
}
