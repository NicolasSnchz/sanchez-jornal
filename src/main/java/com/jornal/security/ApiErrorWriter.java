package com.jornal.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jornal.dto.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Respuestas 401 y 403 de la API en formato ApiError, iguales a las del resto de errores.
 * Sin esto Spring Security responderia con una pagina HTML o un cuerpo vacio.
 */
@Component
public class ApiErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final Clock clock;

    public ApiErrorWriter(ObjectMapper objectMapper, MessageSource messageSource, Clock clock) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        Object jwtError = request.getAttribute(JwtAuthFilter.ERROR_ATTRIBUTE);
        String code = jwtError != null ? jwtError.toString() : "api.autenticacion.requerida";
        write(request, response, HttpStatus.UNAUTHORIZED, code);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "api.acceso.denegado");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code)
            throws IOException {
        String message = messageSource.getMessage(code, null, code, request.getLocale());
        ApiError body = new ApiError(OffsetDateTime.now(clock), status.value(), status.getReasonPhrase(),
                ApiError.toApiCode(code), message, request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
