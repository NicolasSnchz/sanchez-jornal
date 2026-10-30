package com.jornal.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos colombianos para los textos que arma el servidor (por ejemplo, los
 * pendientes del panel): pesos con punto de miles y fechas dia/mes/ano.
 */
public final class Formats {

    public static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", COLOMBIA);

    private Formats() {
    }

    /** 1234567 -> "$1.234.567". */
    public static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(COLOMBIA);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return "$" + format.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }
}
