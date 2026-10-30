package com.jornal.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un valor en pesos a letras, como se escribe en un comprobante de
 * pago: 120000 -> "ciento veinte mil pesos", 1000000 -> "un millon de pesos".
 *
 * <p>Reglas del espanol que aplica: "cien" solo para el 100 exacto; "uno" se
 * acorta a "un" antes de "mil", "millones" y "pesos" (veintiun mil, treinta y un
 * pesos); "de pesos" despues de millones exactos. Las tildes van como escapes
 * Unicode para mantener el codigo fuente en ASCII.
 */
public final class AmountInWords {

    public static final long MAX = 999_999_999_999L;

    private static final String[] UNITS = {
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "diecis\u00e9is", "diecisiete", "dieciocho",
        "diecinueve", "veinte", "veintiuno", "veintid\u00f3s", "veintitr\u00e9s", "veinticuatro", "veinticinco",
        "veintis\u00e9is", "veintisiete", "veintiocho", "veintinueve"
    };

    private static final String[] TENS = {
        "", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    private static final String[] HUNDREDS = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos",
        "setecientos", "ochocientos", "novecientos"
    };

    private AmountInWords() {
    }

    /** Valor en pesos enteros (se redondea al peso) escrito en letras. */
    public static String pesos(BigDecimal amount) {
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (value < 0 || value > MAX) {
            throw new IllegalArgumentException("Valor fuera de rango: " + value);
        }
        if (value == 1) {
            return "un peso";
        }
        boolean exactMillions = value >= 1_000_000 && value % 1_000_000 == 0;
        return shorten(words(value)) + (exactMillions ? " de pesos" : " pesos");
    }

    /** Numero en letras, sin moneda. */
    static String words(long value) {
        if (value == 0) {
            return UNITS[0];
        }
        long millions = value / 1_000_000;
        long rest = value % 1_000_000;
        StringBuilder text = new StringBuilder();
        if (millions == 1) {
            text.append("un mill\u00f3n");
        } else if (millions > 1) {
            text.append(shorten(belowMillion(millions))).append(" millones");
        }
        if (rest > 0) {
            appendWord(text, belowMillion(rest));
        }
        return text.toString();
    }

    private static String belowMillion(long value) {
        long thousands = value / 1000;
        long rest = value % 1000;
        StringBuilder text = new StringBuilder();
        if (thousands == 1) {
            text.append("mil");
        } else if (thousands > 1) {
            text.append(shorten(belowThousand((int) thousands))).append(" mil");
        }
        if (rest > 0) {
            appendWord(text, belowThousand((int) rest));
        }
        return text.toString();
    }

    private static String belowThousand(int value) {
        if (value == 100) {
            return "cien";
        }
        StringBuilder text = new StringBuilder(HUNDREDS[value / 100]);
        int rest = value % 100;
        if (rest > 0) {
            appendWord(text, belowHundred(rest));
        }
        return text.toString();
    }

    private static String belowHundred(int value) {
        if (value < UNITS.length) {
            return UNITS[value];
        }
        int unit = value % 10;
        return TENS[value / 10] + (unit > 0 ? " y " + UNITS[unit] : "");
    }

    /** "uno" pierde la ultima vocal antes de un sustantivo: veintiuno -> veintiun, uno -> un. */
    private static String shorten(String text) {
        if (text.endsWith("veintiuno")) {
            return text.substring(0, text.length() - "veintiuno".length()) + "veinti\u00fan";
        }
        if (text.endsWith("uno")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static void appendWord(StringBuilder text, String word) {
        if (!text.isEmpty()) {
            text.append(' ');
        }
        text.append(word);
    }
}
