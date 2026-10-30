package com.jornal.exception;

import com.jornal.controller.support.MessageArgs;
import com.jornal.dto.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce excepciones a respuestas ApiError para /api/**. Cada caso tiene su
 * codigo HTTP: 400 datos mal formados, 401 credenciales del login, 404 no
 * existe (o no es tuyo), 409
 * conflicto de concurrencia, 422 regla de negocio incumplida y 500 lo inesperado,
 * que se registra completo en el log pero nunca se describe al cliente.
 */
@RestControllerAdvice(basePackages = "com.jornal.controller.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final MessageSource messageSource;
    private final Clock clock;

    public ApiExceptionHandler(MessageSource messageSource, Clock clock) {
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ApiError.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(),
                        messageSource.getMessage(error, LocaleContextHolder.getLocale())))
                .toList();
        List<ApiError.FieldError> global = e.getBindingResult().getGlobalErrors().stream()
                .map(error -> new ApiError.FieldError(error.getObjectName(),
                        messageSource.getMessage(error, LocaleContextHolder.getLocale())))
                .toList();
        List<ApiError.FieldError> all = new ArrayList<>(fields);
        all.addAll(global);
        return build(HttpStatus.BAD_REQUEST, "api.validacion", new Object[0], request, all);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable(Exception e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "api.peticion.invalida", new Object[0], request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "api.metodo.no.permitido", new Object[0], request, List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getCode(), new Object[0], request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> businessRule(BusinessRuleException e, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e.getCode(), e.getArgs(), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concurrency(ObjectOptimisticLockingFailureException e,
                                                HttpServletRequest request) {
        log.warn("Conflicto de concurrencia en {}", request.getRequestURI());
        return build(HttpStatus.CONFLICT, "conflicto.concurrente", new Object[0], request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Restriccion de datos violada en {}", request.getRequestURI());
        return build(HttpStatus.CONFLICT, "conflicto.datos", new Object[0], request, List.of());
    }

    /**
     * Login de la API con credenciales invalidas. Correo inexistente y clave errada
     * dan el mismo mensaje para no revelar que cuentas existen.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException e, HttpServletRequest request) {
        String code = e instanceof LockedException ? "api.cuenta.bloqueada"
                : e instanceof DisabledException ? "api.cuenta.inactiva" : "api.credenciales.invalidas";
        return build(HttpStatus.UNAUTHORIZED, code, new Object[0], request, List.of());
    }

    /** @PreAuthorize lanza esta excepcion dentro del controlador; se responde 403 como lo haria el filtro. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "api.acceso.denegado", new Object[0], request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.inesperado", new Object[0], request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, Object[] args, HttpServletRequest request,
                                           List<ApiError.FieldError> fieldErrors) {
        String message = messageSource.getMessage(code, MessageArgs.format(args), code, LocaleContextHolder.getLocale());
        ApiError body = new ApiError(OffsetDateTime.now(clock), status.value(), status.getReasonPhrase(),
                ApiError.toApiCode(code), message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
