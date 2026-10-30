package com.jornal.exception;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.controller.support.SafeRedirect;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Errores de las paginas web. Una regla de negocio incumplida en una accion
 * (confirmar, anular...) vuelve a la pagina anterior con el aviso; los
 * formularios capturan la suya para mostrarla junto a los datos digitados.
 */
@ControllerAdvice(basePackages = "com.jornal.controller.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public String businessRule(BusinessRuleException e, HttpServletRequest request) {
        return redirectBackWith(request, new Notice("error", e.getCode(), MessageArgs.format(e.getArgs())));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String concurrency(ObjectOptimisticLockingFailureException e, HttpServletRequest request) {
        log.warn("Conflicto de concurrencia en {}", request.getRequestURI());
        return redirectBackWith(request, new Notice("error", "conflicto.concurrente", new Object[0]));
    }

    /** Un recurso ajeno, inexistente o con un id mal formado se ve igual: no existe. */
    @ExceptionHandler({ResourceNotFoundException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "error/404";
    }

    private static String redirectBackWith(HttpServletRequest request, Notice notice) {
        RequestContextUtils.getOutputFlashMap(request).put(Notice.ATTRIBUTE, notice);
        return "redirect:" + SafeRedirect.back(request, "/");
    }
}
