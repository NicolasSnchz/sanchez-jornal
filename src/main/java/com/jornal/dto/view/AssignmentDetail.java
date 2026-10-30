package com.jornal.dto.view;

import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.SiteStatus;
import com.jornal.entity.Trade;
import com.jornal.util.TimeScale;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Todo lo que muestra la pantalla de una asignacion. Los totales y las acciones
 * permitidas los calcula el dominio; la vista solo los pinta.
 */
public record AssignmentDetail(
        UUID id,
        AssignmentStatus status,
        // trabajador
        UUID workerId,
        String workerName,
        String workerInitials,
        Trade workerTrade,
        int workerYearsOfExperience,
        String workerPhone,
        String workerEmergencyPhone,
        LocalDate workerArlValidUntil,
        // maestro y obra
        String foremanName,
        String foremanInitials,
        String foremanCompany,
        String foremanPhone,
        UUID siteId,
        String siteName,
        String siteAddress,
        String siteNeighborhood,
        String siteCity,
        SiteStatus siteStatus,
        UUID frontId,
        String frontTask,
        Trade requiredTrade,
        Set<DayOfWeek> scheduleDays,
        LocalTime startTime,
        LocalTime endTime,
        int lunchBreakMinutes,
        int scheduledMinutesPerDay,
        LocalDate frontStartDate,
        LocalDate frontEndDate,
        // pacto
        BigDecimal offeredDailyWage,
        BigDecimal dailyWage,
        BigDecimal hourlyRate,
        BigDecimal overtimeRate,
        LocalDate startDate,
        LocalDate actualEndDate,
        LocalDateTime invitedAt,
        LocalDateTime respondedAt,
        Integer workerRating,
        String workerRatingComment,
        Integer foremanRating,
        String cancellationReason,
        // dinero
        BigDecimal earnedConfirmed,
        BigDecimal earnedUnconfirmed,
        BigDecimal paidTotal,
        BigDecimal balance,
        LocalDate lastSettledDate,
        LocalDate nextUnsettledDate,
        boolean finalSettled,
        // detalle
        long workedDays,
        long pendingDays,
        long disputedDays,
        List<WorkDayRow> workDays,
        List<PaymentRow> payments,
        WeekView week,
        // acciones
        boolean canRegisterWorkDay,
        boolean canRegisterAdvance,
        boolean canSettlePeriod,
        boolean canFinalSettle,
        boolean canComplete,
        boolean canCancel,
        boolean canRespond,
        boolean canRateForeman) {

    public BigDecimal earnedTotal() {
        return earnedConfirmed.add(earnedUnconfirmed);
    }

    /** Franja del horario pactado en la escala de la planilla (porcentajes). */
    public String scheduleStart() {
        return TimeScale.position(startTime);
    }

    public String scheduleLength() {
        return TimeScale.length((int) Duration.between(startTime, endTime).toMinutes());
    }
}
