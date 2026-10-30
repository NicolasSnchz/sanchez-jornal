package com.jornal.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Escala horizontal de la jornada diurna (06:00 a 19:00) para dibujar las barras
 * de la planilla. Devuelve porcentajes como texto para atributos data-*; el
 * navegador los aplica con JavaScript porque la CSP no permite estilos en linea.
 */
public final class TimeScale {

    private static final int START = 6 * 60;
    private static final int SPAN = 13 * 60;

    private TimeScale() {
    }

    public static String position(LocalTime time) {
        return percent(time.getHour() * 60 + time.getMinute() - START);
    }

    public static String length(int minutes) {
        return percent(minutes);
    }

    private static String percent(int minutes) {
        int clamped = Math.max(0, Math.min(SPAN, minutes));
        return BigDecimal.valueOf(clamped * 100L).divide(BigDecimal.valueOf(SPAN), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
