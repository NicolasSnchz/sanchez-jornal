package com.jornal.controller.support;

import com.jornal.util.Formats;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Formatos para las plantillas, disponibles como {@code ${@fmt.money(valor)}}.
 * Centralizarlos evita que cada vista formatee distinto el dinero o las horas.
 *
 * <p>Las abreviaturas de meses y dias, y "a. m." / "p. m.", se definen aqui y no
 * se toman del JDK: cada version trae datos de idioma distintos (Java 17 escribe
 * "sep." y Java 21 "sept"), y la aplicacion debe verse igual en cualquiera.
 */
@Component("fmt")
public class ViewFormats {

    private static final Map<Long, String> SHORT_MONTHS = Map.ofEntries(
            Map.entry(1L, "ene"), Map.entry(2L, "feb"), Map.entry(3L, "mar"), Map.entry(4L, "abr"),
            Map.entry(5L, "may"), Map.entry(6L, "jun"), Map.entry(7L, "jul"), Map.entry(8L, "ago"),
            Map.entry(9L, "sept"), Map.entry(10L, "oct"), Map.entry(11L, "nov"), Map.entry(12L, "dic"));
    private static final Map<Long, String> MONTHS = Map.ofEntries(
            Map.entry(1L, "enero"), Map.entry(2L, "febrero"), Map.entry(3L, "marzo"), Map.entry(4L, "abril"),
            Map.entry(5L, "mayo"), Map.entry(6L, "junio"), Map.entry(7L, "julio"), Map.entry(8L, "agosto"),
            Map.entry(9L, "septiembre"), Map.entry(10L, "octubre"), Map.entry(11L, "noviembre"),
            Map.entry(12L, "diciembre"));
    private static final Map<Long, String> SHORT_DAYS = Map.of(1L, "lun", 2L, "mar", 3L, "mi\u00e9", 4L, "jue",
            5L, "vie", 6L, "s\u00e1b", 7L, "dom");
    private static final Map<Long, String> DAYS = Map.of(1L, "lunes", 2L, "martes", 3L, "mi\u00e9rcoles",
            4L, "jueves", 5L, "viernes", 6L, "s\u00e1bado", 7L, "domingo");
    /** Con espacio de no separacion para que "p. m." nunca quede partido en dos lineas. */
    private static final Map<Long, String> AM_PM = Map.of(0L, "a.\u00a0m.", 1L, "p.\u00a0m.");

    private static final DateTimeFormatter DAY = builder().appendPattern("d ")
            .appendText(ChronoField.MONTH_OF_YEAR, SHORT_MONTHS).appendPattern(" uuuu").toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter DAY_SHORT = builder().appendPattern("d ")
            .appendText(ChronoField.MONTH_OF_YEAR, SHORT_MONTHS).toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter WEEKDAY = builder().appendText(ChronoField.DAY_OF_WEEK, SHORT_DAYS)
            .appendPattern(" d").toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter WEEKDAY_DATE = builder().appendText(ChronoField.DAY_OF_WEEK, SHORT_DAYS)
            .appendPattern(" d ").appendText(ChronoField.MONTH_OF_YEAR, SHORT_MONTHS).toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter WEEKDAY_LONG = builder().appendText(ChronoField.DAY_OF_WEEK, DAYS)
            .appendPattern(" d 'de' ").appendText(ChronoField.MONTH_OF_YEAR, MONTHS).toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter TIME = builder().appendPattern("h:mm ")
            .appendText(ChronoField.AMPM_OF_DAY, AM_PM).toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter DATE_TIME = builder().append(DAY).appendLiteral(", ").append(TIME)
            .toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter MONTH = builder().appendText(ChronoField.MONTH_OF_YEAR, MONTHS)
            .appendPattern(" 'de' uuuu").toFormatter(Formats.COLOMBIA);
    private static final DateTimeFormatter MONTH_NAME = builder().appendText(ChronoField.MONTH_OF_YEAR, MONTHS)
            .toFormatter(Formats.COLOMBIA);

    private static DateTimeFormatterBuilder builder() {
        return new DateTimeFormatterBuilder();
    }

    public String money(BigDecimal amount) {
        return amount == null ? "" : Formats.money(amount);
    }

    public String date(LocalDate date) {
        return date == null ? "" : date.format(DAY);
    }

    public String shortDate(LocalDate date) {
        return date == null ? "" : date.format(DAY_SHORT);
    }

    public String weekday(LocalDate date) {
        return date == null ? "" : date.format(WEEKDAY);
    }

    /** "jue 29 oct": para las filas de las tablas. */
    public String weekdayDate(LocalDate date) {
        return date == null ? "" : date.format(WEEKDAY_DATE);
    }

    public String longDay(LocalDate date) {
        return date == null ? "" : capitalize(date.format(WEEKDAY_LONG));
    }

    public String time(LocalTime time) {
        return time == null ? "" : time.format(TIME);
    }

    public String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME);
    }

    public String month(YearMonth month) {
        return month == null ? "" : capitalize(month.format(MONTH));
    }

    /** Nombre del mes en minuscula, para usarlo dentro de una frase: "octubre". */
    public String monthName(YearMonth month) {
        return month == null ? "" : month.format(MONTH_NAME);
    }

    /** 510 -> "8 h 30 min"; 480 -> "8 h"; 45 -> "45 min". */
    public String duration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours == 0) {
            return rest + " min";
        }
        return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
    }

    /** Dias laborales en texto corto: "lun a vie" si son seguidos, si no "lun, mie, vie". */
    public String days(Collection<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return "";
        }
        var sorted = days.stream().sorted().toList();
        DayOfWeek first = sorted.get(0);
        DayOfWeek last = sorted.get(sorted.size() - 1);
        boolean consecutive = last.getValue() - first.getValue() == sorted.size() - 1;
        if (consecutive && sorted.size() > 2) {
            return dayName(first) + " a " + dayName(last);
        }
        return sorted.stream().map(this::dayName).collect(Collectors.joining(", "));
    }

    public String dayName(DayOfWeek day) {
        return SHORT_DAYS.get((long) day.getValue());
    }

    public String percent(BigDecimal fraction) {
        return fraction == null ? "" : fraction.movePointRight(2).stripTrailingZeros().toPlainString() + " %";
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
