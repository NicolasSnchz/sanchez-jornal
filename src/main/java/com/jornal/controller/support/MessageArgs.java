package com.jornal.controller.support;

import com.jornal.util.Formats;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Prepara los argumentos de un mensaje antes de mostrarlo: en este dominio un
 * BigDecimal siempre es dinero y una fecha se muestra dia/mes/ano.
 */
public final class MessageArgs {

    private MessageArgs() {
    }

    public static Object[] format(Object... args) {
        if (args == null) {
            return new Object[0];
        }
        Object[] formatted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof BigDecimal amount) {
                formatted[i] = Formats.money(amount);
            } else if (arg instanceof LocalDate date) {
                formatted[i] = Formats.date(date);
            } else {
                formatted[i] = arg;
            }
        }
        return formatted;
    }
}
