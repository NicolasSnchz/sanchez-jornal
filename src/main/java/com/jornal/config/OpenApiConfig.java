package com.jornal.config;

import com.jornal.dto.api.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Contrato OpenAPI generado desde el codigo (springdoc): la documentacion no se
 * escribe aparte, asi nunca queda desactualizada. Swagger UI solo se publica con
 * los perfiles dev y demo.
 *
 * <p>Las respuestas de error se agregan aqui una sola vez y no con anotaciones
 * repetidas en cada metodo: todas salen del mismo ApiExceptionHandler y tienen el
 * mismo cuerpo ApiError, asi que repetirlas en 26 operaciones solo invitaria a
 * que alguna quedara distinta.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Jornal API",
        version = "1.0.0",
        description = "API REST de Jornal para clientes m\u00f3viles: los maestros registran jornadas y pagos "
                + "desde la obra y los trabajadores confirman sus jornadas y pagos. "
                + "Autenticaci\u00f3n sin estado con JWT: primero POST /api/auth/login y luego el header "
                + "Authorization: Bearer {token} (bot\u00f3n Authorize). "
                + "Todos los errores responden con el mismo cuerpo ApiError.",
        contact = @Contact(name = "Nicol\u00e1s S\u00e1nchez", email = "nicosanchezvil@gmail.com")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    private static final String API_ERROR_REF = "#/components/schemas/ApiError";

    /** Registra el esquema ApiError para que las respuestas de error puedan referenciarlo. */
    @Bean
    public OpenApiCustomizer apiErrorSchema() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            ModelConverters.getInstance().read(ApiError.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
        };
    }

    /**
     * Agrega a cada operacion las respuestas de error que de verdad puede dar,
     * segun lo que recibe: cuerpo (400), id en la ruta (404), token (401 y 403) y
     * cambios de un usuario autenticado (409 y 422). Las respuestas que ya declara
     * el metodo no se tocan.
     */
    @Bean
    public OperationCustomizer standardErrorResponses() {
        return (operation, handler) -> {
            ApiResponses responses = operation.getResponses();
            boolean changesData = handler.hasMethodAnnotation(PostMapping.class)
                    || handler.hasMethodAnnotation(PutMapping.class)
                    || handler.hasMethodAnnotation(DeleteMapping.class);
            boolean hasBody = Arrays.stream(handler.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(RequestBody.class));
            boolean hasId = Arrays.stream(handler.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(PathVariable.class));
            boolean secured = operation.getSecurity() != null && !operation.getSecurity().isEmpty();

            if (hasBody) {
                addError(responses, "400", "Datos inv\u00e1lidos: el cuerpo trae la lista de campos con su mensaje");
            }
            if (secured) {
                addError(responses, "401", "Falta el token, est\u00e1 alterado o ya venci\u00f3");
                addError(responses, "403", "El token es v\u00e1lido pero el rol no puede usar esta operaci\u00f3n");
            }
            if (hasId) {
                addError(responses, "404", "No existe o no le pertenece a quien hace la petici\u00f3n");
            }
            if (changesData && secured) {
                addError(responses, "409", "Otro cambio sobre los mismos datos se guard\u00f3 primero; reintentar");
                addError(responses, "422", "La petici\u00f3n es v\u00e1lida pero incumple una regla del negocio");
            }
            addError(responses, "500", "Error inesperado; el detalle queda solo en el log del servidor");
            return operation;
        };
    }

    private static void addError(ApiResponses responses, String code, String description) {
        if (responses.containsKey(code)) {
            return;
        }
        Schema<?> schema = new Schema<>().$ref(API_ERROR_REF);
        responses.addApiResponse(code, new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema))));
    }
}
