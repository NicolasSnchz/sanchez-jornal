package com.jornal.dto.view;

import java.time.LocalDate;

/**
 * Un dia de la semana en el "muro" de jornadas: puede ser un dia trabajado,
 * un dia laboral sin registro, un dia no laboral o un dia futuro.
 */
public record WeekDayCell(
        LocalDate date,
        boolean workingDay,
        boolean withinAssignment,
        boolean future,
        boolean today,
        WorkDayRow workDay,
        String holiday) {

    public boolean isMissing() {
        return workingDay && withinAssignment && !future && workDay == null;
    }
}
