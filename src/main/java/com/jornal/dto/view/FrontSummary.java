package com.jornal.dto.view;

import com.jornal.entity.Trade;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/** Frente de trabajo listo para mostrar: cupos, horario y tarifas ya calculados. */
public record FrontSummary(
        UUID id,
        String task,
        Trade requiredTrade,
        int openings,
        long activeAssignments,
        int availableOpenings,
        BigDecimal offeredDailyWage,
        LocalDate startDate,
        LocalDate endDate,
        Set<DayOfWeek> workDays,
        LocalTime startTime,
        LocalTime endTime,
        int lunchBreakMinutes,
        int scheduledMinutesPerDay,
        int scheduledMinutesPerWeek,
        BigDecimal hourlyRate,
        BigDecimal overtimeRate,
        boolean canInvite) {

    /** Porcentaje de cupos ocupados, para la barra de progreso. */
    public int occupancyPercent() {
        return openings == 0 ? 0 : (int) Math.round(activeAssignments * 100.0 / openings);
    }
}
