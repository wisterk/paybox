package me.wisterk.paybox.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI (Swagger) для документирования REST API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Online Payment System API",
                version = "1.0",
                description = "Payment gateway with T-Bank integration (Invoice, SBP QR)"
        )
)
@SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        paramName = "X-API-Key",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
