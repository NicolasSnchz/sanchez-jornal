package com.jornal.controller.support;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Aviso que sobrevive a una redireccion (patron Post/Redirect/Get) y se muestra
 * una sola vez en la siguiente pagina.
 *
 * @param kind success, error o info
 * @param code codigo del mensaje en messages.properties
 */
public record Notice(String kind, String code, Object[] args) {

    public static final String ATTRIBUTE = "notice";

    public static void success(RedirectAttributes attributes, String code, Object... args) {
        attributes.addFlashAttribute(ATTRIBUTE, new Notice("success", code, MessageArgs.format(args)));
    }

    public static void error(RedirectAttributes attributes, String code, Object... args) {
        attributes.addFlashAttribute(ATTRIBUTE, new Notice("error", code, MessageArgs.format(args)));
    }
}
