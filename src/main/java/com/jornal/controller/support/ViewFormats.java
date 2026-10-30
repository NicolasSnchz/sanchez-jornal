package com.jornal.controller.support;

import com.jornal.util.Formats;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Formatos para las plantillas, disponibles como {@code ${@fmt.money(valor)}}.
 * Centralizarlos evita que cada vista formatee distinto el dinero o las horas.
 */
@Component("fmt")
public class ViewFormats {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Formats.COLOMBIA);
    private static final DateTimeFormatter DAY_SHORT = DateTimeFormatter.ofPattern("d MMM", Formats.COLOMBIA);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE d", Formats.COLOMBIA);
    private static final DateTimeFormatter WEEKDAY_LONG = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",
            Formats.COLOMBIA);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Formats.COLOMBIA);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a",
            Formats.COLOMBIA);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Formats.COLOMBIA);
    private static final DateTimeFormatter MONTH_NAME = DateTimeFormatter.ofPattern("MMMM", Formats.COLOMBIA);
    private static final DateTimeFormatter WEEKDAY_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Formats.COLOMBIA);

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
        return day.getDisplayName(TextStyle.SHORT, Formats.COLOMBIA).replace(".", "");
    }

    public String percent(BigDecimal fraction) {
        return fraction == null ? "" : fraction.movePointRight(2).stripTrailingZeros().toPlainString() + " %";
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
