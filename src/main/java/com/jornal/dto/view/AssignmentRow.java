package com.jornal.dto.view;

import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Trade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de una tabla de asignaciones (vista del maestro o del trabajador). */
public record AssignmentRow(
        UUID id,
        UUID workerId,
        String workerName,
        String workerInitials,
        Trade workerTrade,
        String foremanName,
        String siteName,
        String frontTask,
        AssignmentStatus status,
        BigDecimal dailyWage,
        LocalDate startDate,
        LocalDate plannedEndDate,
        LocalDateTime invitedAt,
        long workedDays,
        long pendingDays,
        long disputedDays,
        BigDecimal earnedTotal,
        BigDecimal balance,
        Integer workerRating,
        Integer foremanRating) {
}
