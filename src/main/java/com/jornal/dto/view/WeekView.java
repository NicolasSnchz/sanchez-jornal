package com.jornal.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Semana de jornadas de una asignacion, con navegacion a la anterior y la siguiente. */
public record WeekView(
        LocalDate weekStart,
        LocalDate weekEnd,
        LocalDate previousWeek,
        LocalDate nextWeek,
        boolean hasPrevious,
        boolean hasNext,
        List<WeekDayCell> days,
        int workedMinutes,
        int overtimeMinutes,
        BigDecimal value,
        boolean settled) {
}
