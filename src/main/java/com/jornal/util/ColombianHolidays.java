package com.jornal.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Festivos de Colombia, calculados (no escritos a mano por ano):
 * <ul>
 *   <li>Fijos: 1 de enero, 1 de mayo, 20 de julio, 7 de agosto, 8 y 25 de diciembre.</li>
 *   <li>Trasladables al lunes siguiente (Ley 51 de 1983, "Ley Emiliani"): Reyes,
 *       San Jose, San Pedro, Asuncion, Dia de la Raza, Todos los Santos e
 *       Independencia de Cartagena.</li>
 *   <li>Segun la Pascua: Jueves y Viernes Santo, y Ascension, Corpus Christi y
 *       Sagrado Corazon (trasladados a lunes).</li>
 * </ul>
 * Jornal no registra trabajo en festivos: llevan recargo del 90 % (Ley 2466 de
 * 2025) y el calculo de la jornada diurna ordinaria no aplica.
 */
public final class ColombianHolidays {

    private static final Map<Integer, Map<LocalDate, String>> CACHE = new ConcurrentHashMap<>();

    private ColombianHolidays() {
    }

    public static boolean isHoliday(LocalDate date) {
        return holidaysOf(date.getYear()).containsKey(date);
    }

    public static Optional<String> nameOf(LocalDate date) {
        return Optional.ofNullable(holidaysOf(date.getYear()).get(date));
    }

    /** Festivos del ano en orden, con su nombre. */
    public static Map<LocalDate, String> holidaysOf(int year) {
        return CACHE.computeIfAbsent(year, ColombianHolidays::calculate);
    }

    private static Map<LocalDate, String> calculate(int year) {
        Map<LocalDate, String> days = new TreeMap<>();
        fixed(days, year, 1, 1, "A\u00f1o Nuevo");
        fixed(days, year, 5, 1, "D\u00eda del Trabajo");
        fixed(days, year, 7, 20, "Independencia de Colombia");
        fixed(days, year, 8, 7, "Batalla de Boyac\u00e1");
        fixed(days, year, 12, 8, "Inmaculada Concepci\u00f3n");
        fixed(days, year, 12, 25, "Navidad");

        movedToMonday(days, year, 1, 6, "Reyes Magos");
        movedToMonday(days, year, 3, 19, "San Jos\u00e9");
        movedToMonday(days, year, 6, 29, "San Pedro y San Pablo");
        movedToMonday(days, year, 8, 15, "Asunci\u00f3n de la Virgen");
        movedToMonday(days, year, 10, 12, "D\u00eda de la Raza");
        movedToMonday(days, year, 11, 1, "Todos los Santos");
        movedToMonday(days, year, 11, 11, "Independencia de Cartagena");

        LocalDate easter = easterSunday(year);
        days.put(easter.minusDays(3), "Jueves Santo");
        days.put(easter.minusDays(2), "Viernes Santo");
        days.put(nextMonday(easter.plusDays(39)), "Ascensi\u00f3n del Se\u00f1or");
        days.put(nextMonday(easter.plusDays(60)), "Corpus Christi");
        days.put(nextMonday(easter.plusDays(68)), "Sagrado Coraz\u00f3n");
        return days;
    }

    /** Domingo de Pascua por el algoritmo gregoriano anonimo (Meeus/Jones/Butcher). */
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private static void fixed(Map<LocalDate, String> days, int year, int month, int day, String name) {
        days.put(MonthDay.of(month, day).atYear(year), name);
    }

    private static void movedToMonday(Map<LocalDate, String> days, int year, int month, int day, String name) {
        days.put(nextMonday(LocalDate.of(year, month, day)), name);
    }

    private static LocalDate nextMonday(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }
}
