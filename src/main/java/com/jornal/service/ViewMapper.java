package com.jornal.service;

import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.AssignmentRow;
import com.jornal.dto.view.FrontSummary;
import com.jornal.dto.view.PaymentRow;
import com.jornal.dto.view.SiteDetail;
import com.jornal.dto.view.SiteSummary;
import com.jornal.dto.view.WeekDayCell;
import com.jornal.dto.view.WeekView;
import com.jornal.dto.view.WorkDayRow;
import com.jornal.entity.Advance;
import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentStatus;
import com.jornal.entity.Settlement;
import com.jornal.entity.SiteStatus;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkDayStatus;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.util.ColombianHolidays;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Convierte entidades en modelos de vista dentro de la transaccion del servicio.
 * Las vistas nunca reciben entidades JPA: asi no hay consultas perezosas desde la
 * plantilla (Open Session In View esta desactivado) ni datos de mas expuestos.
 */
@Component
public class ViewMapper {

    private final Clock clock;

    public ViewMapper(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ------------------------------------------------------------------ obras y frentes

    public SiteSummary toSummary(ConstructionSite site) {
        return new SiteSummary(site.getId(), site.getName(), site.getAddress(), site.getNeighborhood(),
                site.getCity(), site.getStatus(), site.isDelayed(today()), site.getStartDate(),
                site.getEstimatedEndDate(), site.getFronts().size(), site.countWorkersInProgress(),
                site.calculateLaborCost());
    }

    public SiteDetail toDetail(ConstructionSite site) {
        SiteStatus status = site.getStatus();
        List<AssignmentRow> rows = site.getFronts().stream()
                .flatMap(front -> front.getAssignments().stream())
                .filter(a -> a.getStatus() != AssignmentStatus.REJECTED && a.getStatus() != AssignmentStatus.CANCELLED)
                .sorted(Comparator.comparing((Assignment a) -> a.getStatus().ordinal())
                        .thenComparing(a -> a.getWorker().getLastName()))
                .map(this::toRow)
                .toList();
        long active = site.countActiveAssignments();
        return new SiteDetail(site.getId(), site.getName(), site.getDescription(), site.getAddress(),
                site.getNeighborhood(), site.getCity(), site.getBuildingPermit(), site.getStartDate(),
                site.getEstimatedEndDate(), status, site.isDelayed(today()), site.calculateLaborCost(), active,
                site.countWorkersInProgress(), site.getFronts().stream().map(this::toSummary).toList(), rows,
                status != SiteStatus.FINISHED,
                status == SiteStatus.PLANNED,
                status == SiteStatus.IN_PROGRESS,
                status == SiteStatus.SUSPENDED,
                status == SiteStatus.IN_PROGRESS && active == 0,
                status != SiteStatus.FINISHED);
    }

    public FrontSummary toSummary(WorkFront front) {
        BigDecimal wage = front.getOfferedDailyWage();
        BigDecimal hourly = front.hourlyRate(wage);
        SiteStatus siteStatus = front.getSite().getStatus();
        boolean canInvite = front.availableOpenings() > 0 && !front.getEndDate().isBefore(today())
                && (siteStatus == SiteStatus.PLANNED || siteStatus == SiteStatus.IN_PROGRESS);
        return new FrontSummary(front.getId(), front.getTask(), front.getRequiredTrade(), front.getOpenings(),
                front.countActiveAssignments(), front.availableOpenings(), wage, front.getStartDate(),
                front.getEndDate(), front.getWorkDays(), front.getStartTime(), front.getEndTime(),
                front.getLunchBreakMinutes(), front.scheduledMinutesPerDay(), front.scheduledMinutesPerWeek(),
                hourly, overtimeRate(hourly), canInvite);
    }

    // ------------------------------------------------------------------ asignaciones

    public AssignmentRow toRow(Assignment a) {
        Worker worker = a.getWorker();
        WorkFront front = a.getFront();
        return new AssignmentRow(a.getId(), worker.getId(), worker.getFullName(), worker.getInitials(),
                worker.getTrade(), front.getSite().getForeman().getFullName(), front.getSite().getName(),
                front.getTask(), a.getStatus(), a.getDailyWage(), a.getStartDate(), a.plannedEndDate(),
                a.getInvitedAt(), a.getWorkDays().size(),
                a.countDays(d -> d.getStatus() == WorkDayStatus.PENDING),
                a.countDays(d -> d.getStatus() == WorkDayStatus.DISPUTED),
                a.calculateEarnedTotal(), a.calculateBalance(), a.getWorkerRating(), a.getForemanRating());
    }

    /**
     * @param requestedWeek cualquier dia de la semana que se quiere ver; null para la semana por defecto
     */
    public AssignmentDetail toDetail(Assignment a, LocalDate requestedWeek) {
        Worker worker = a.getWorker();
        WorkFront front = a.getFront();
        ConstructionSite site = front.getSite();
        Foreman foreman = site.getForeman();
        AssignmentStatus status = a.getStatus();
        boolean inProgress = status == AssignmentStatus.IN_PROGRESS;
        boolean finalSettled = a.hasEffectiveFinalSettlement();
        long unconfirmed = a.countDays(d -> d.getStatus() != WorkDayStatus.CONFIRMED);
        BigDecimal balance = a.calculateBalance();
        BigDecimal hourly = front.hourlyRate(a.getDailyWage());
        Optional<LocalDate> settledUntil = a.lastSettledDate();

        List<WorkDayRow> days = a.getWorkDays().stream()
                .sorted(Comparator.comparing(WorkDay::getDate).reversed())
                .map(day -> toRow(a, day, settledUntil))
                .toList();
        List<PaymentRow> payments = a.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getPaidAt).reversed())
                .map(this::toRow)
                .toList();

        return new AssignmentDetail(a.getId(), status,
                worker.getId(), worker.getFullName(), worker.getInitials(), worker.getTrade(),
                worker.getYearsOfExperience(), worker.getPhone(), worker.getEmergencyPhone(),
                worker.getArlValidUntil(),
                foreman.getFullName(), foreman.getInitials(), foreman.getCompanyName(), foreman.getPhone(),
                site.getId(), site.getName(), site.getAddress(), site.getNeighborhood(), site.getCity(),
                site.getStatus(), front.getId(), front.getTask(), front.getRequiredTrade(), front.getWorkDays(),
                front.getStartTime(), front.getEndTime(), front.getLunchBreakMinutes(),
                front.scheduledMinutesPerDay(), front.getStartDate(), front.getEndDate(),
                front.getOfferedDailyWage(), a.getDailyWage(), hourly, overtimeRate(hourly),
                a.getStartDate(), a.getActualEndDate(), a.getInvitedAt(), a.getRespondedAt(),
                a.getWorkerRating(), a.getWorkerRatingComment(), a.getForemanRating(), a.getCancellationReason(),
                a.calculateEarnedConfirmed(), a.calculateEarnedUnconfirmed(), a.calculatePaidTotal(), balance,
                settledUntil.orElse(null), a.nextUnsettledDate(), finalSettled,
                a.getWorkDays().size(), a.countDays(d -> d.getStatus() == WorkDayStatus.PENDING),
                a.countDays(d -> d.getStatus() == WorkDayStatus.DISPUTED),
                days, payments, toWeek(a, requestedWeek, settledUntil),
                inProgress && site.getStatus() == SiteStatus.IN_PROGRESS && !finalSettled,
                inProgress && balance.signum() > 0,
                inProgress && !finalSettled && balance.signum() > 0,
                inProgress && !finalSettled && unconfirmed == 0 && balance.signum() > 0,
                inProgress && unconfirmed == 0 && balance.signum() == 0,
                status.isActive() && a.getWorkDays().isEmpty(),
                status == AssignmentStatus.INVITED,
                status == AssignmentStatus.COMPLETED && a.getForemanRating() == null);
    }

    public WorkDayRow toRow(WorkDay day) {
        Assignment a = day.getAssignment();
        return toRow(a, day, a.lastSettledDate());
    }

    private WorkDayRow toRow(Assignment a, WorkDay day, Optional<LocalDate> settledUntil) {
        int scheduled = a.getFront().scheduledMinutesPerDay();
        boolean settled = settledUntil.map(end -> !day.getDate().isAfter(end)).orElse(false);
        boolean editable = a.getStatus() == AssignmentStatus.IN_PROGRESS
                && day.getStatus() != WorkDayStatus.CONFIRMED && !settled;
        return new WorkDayRow(day.getId(), day.getDate(), day.getCheckInTime(), day.getCheckOutTime(),
                day.getBreakMinutes(), day.workedMinutes(), day.overtimeMinutes(scheduled), day.fraction(scheduled),
                a.valueOf(day), day.getStatus(), day.getTaskPerformed(), day.getNotes(), day.getDisputeReason(),
                day.wasCorrectedAfterDispute(), settled, editable);
    }

    /** Semana por defecto: la de hoy si la asignacion sigue abierta; si no, la del ultimo dia trabajado. */
    private WeekView toWeek(Assignment a, LocalDate requested, Optional<LocalDate> settledUntil) {
        LocalDate today = today();
        LocalDate first = a.getStartDate();
        LocalDate last = a.getStatus() == AssignmentStatus.IN_PROGRESS
                ? min(today, a.getFront().getEndDate())
                : a.lastWorkDayDate().orElse(min(today, a.getFront().getEndDate()));
        if (last.isBefore(first)) {
            last = first;
        }
        LocalDate anchor = requested != null ? requested : last;
        if (anchor.isBefore(first)) {
            anchor = first;
        }
        if (anchor.isAfter(last)) {
            anchor = last;
        }
        LocalDate weekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<WeekDayCell> cells = new ArrayList<>();
        int worked = 0;
        int overtime = 0;
        BigDecimal value = BigDecimal.ZERO;
        boolean allSettled = true;
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            boolean scheduledDay = a.getFront().isScheduledDay(date);
            String holiday = scheduledDay ? ColombianHolidays.nameOf(date).orElse(null) : null;
            boolean workingDay = scheduledDay && holiday == null;
            Optional<WorkDay> day = a.findWorkDayOn(date);
            if (!scheduledDay && day.isEmpty()) {
                continue;
            }
            WorkDayRow row = day.map(d -> toRow(a, d, settledUntil)).orElse(null);
            if (row != null) {
                worked += row.workedMinutes();
                overtime += row.overtimeMinutes();
                value = value.add(row.value());
                allSettled &= row.settled();
            } else {
                allSettled = false;
            }
            boolean within = !date.isBefore(a.getStartDate()) && !date.isAfter(a.getFront().getEndDate());
            cells.add(new WeekDayCell(date, workingDay, within, date.isAfter(today), date.equals(today), row, holiday));
        }
        LocalDate previous = weekStart.minusWeeks(1);
        LocalDate next = weekStart.plusWeeks(1);
        return new WeekView(weekStart, weekEnd, previous, next,
                weekStart.isAfter(first),
                !next.isAfter(last), cells, worked, overtime, value, allSettled && !cells.isEmpty()
                && cells.stream().anyMatch(c -> c.workDay() != null));
    }

    // ------------------------------------------------------------------ pagos

    public PaymentRow toRow(Payment p) {
        Assignment a = p.getAssignment();
        String reason = null;
        Boolean requestedByWorker = null;
        LocalDate periodStart = null;
        LocalDate periodEnd = null;
        if (p instanceof Advance advance) {
            reason = advance.getReason();
            requestedByWorker = advance.isRequestedByWorker();
        } else if (p instanceof Settlement settlement) {
            periodStart = settlement.getPeriodStart();
            periodEnd = settlement.getPeriodEnd();
        }
        boolean pending = p.getStatus() == PaymentStatus.PENDING;
        return new PaymentRow(p.getId(), a.getId(), p.getReceiptNumber(), p.getType(), p.getPaidAt(),
                p.getAmount(), p.getMethod(), p.getTransactionReference(), p.getStatus(), reason,
                requestedByWorker, periodStart, periodEnd, p.getCancellationReason(),
                a.getWorker().getFullName(), a.getFront().getSite().getName(), a.getFront().getTask(),
                p.canBeCancelled(), pending);
    }

    // ------------------------------------------------------------------ utilidades

    /** Valor de la hora extra diurna: hora ordinaria mas el recargo del 25 %. */
    public static BigDecimal overtimeRate(BigDecimal hourly) {
        return hourly.multiply(BigDecimal.valueOf(100 + WorkDay.OVERTIME_SURCHARGE_PERCENT))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
