package com.jornal.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI generado desde el codigo (springdoc): la documentacion no se
 * escribe aparte, asi nunca queda desactualizada. Swagger UI solo se publica en dev.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Jornal API",
        version = "1.0.0",
        description = "API REST de Jornal para clientes moviles: los maestros registran jornadas y pagos "
                + "desde la obra y los trabajadores confirman sus jornadas y pagos. "
                + "Autenticacion sin estado con JWT: primero POST /api/auth/login y luego el header "
                + "Authorization: Bearer {token}.",
        contact = @Contact(name = "Nicolas Sanchez", email = "nicosanchezvil@gmail.com")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
