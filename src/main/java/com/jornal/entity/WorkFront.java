package com.jornal.entity;

import com.jornal.exception.BusinessRuleException;
import com.jornal.util.ColombianHolidays;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Frente de trabajo: una labor concreta dentro de una obra, con su oficio,
 * cupos, jornal ofrecido y horario. Es lo que el trabajador acepta.
 *
 * <p>El horario se valida contra la jornada diurna (06:00 a 19:00, articulo 160
 * del CST modificado por la Ley 2466 de 2025) y contra la jornada maxima semanal
 * de 42 horas vigente desde julio de 2026 (Ley 2101 de 2021). Jornal no maneja
 * trabajo nocturno ni dominical: sus recargos son otros y no aplican a la obra diurna.
 */
@Entity
@Table(name = "work_fronts")
public class WorkFront extends BaseEntity {

    public static final LocalTime DAYTIME_START = LocalTime.of(6, 0);
    public static final LocalTime DAYTIME_END = LocalTime.of(19, 0);
    public static final int MAX_WEEKLY_MINUTES = 42 * 60;
    public static final int MAX_LUNCH_MINUTES = 120;
    public static final int MAX_OPENINGS = 50;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private ConstructionSite site;

    @Column(nullable = false, length = 100)
    private String task;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_trade", nullable = false, length = 20)
    private Trade requiredTrade;

    @Column(nullable = false)
    private int openings;

    @Column(name = "offered_daily_wage", nullable = false, precision = 12, scale = 0)
    private BigDecimal offeredDailyWage;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_front_days", joinColumns = @JoinColumn(name = "work_front_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private Set<DayOfWeek> workDays = EnumSet.noneOf(DayOfWeek.class);

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "lunch_break_minutes", nullable = false)
    private int lunchBreakMinutes;

    @OneToMany(mappedBy = "front")
    @OrderBy("invitedAt ASC")
    private List<Assignment> assignments = new ArrayList<>();

    protected WorkFront() {
        // requerido por JPA
    }

    /** Se crea desde {@link ConstructionSite#openFront}, que valida las fechas contra la obra. */
    WorkFront(ConstructionSite site, FrontData data) {
        this.site = Objects.requireNonNull(site, "site");
        validate(data);
        this.task = data.task().trim();
        this.requiredTrade = data.requiredTrade();
        this.openings = data.openings();
        this.offeredDailyWage = data.offeredDailyWage().setScale(0, RoundingMode.HALF_UP);
        this.startDate = data.startDate();
        this.endDate = data.endDate();
        this.workDays = EnumSet.copyOf(data.workDays());
        this.startTime = data.startTime();
        this.endTime = data.endTime();
        this.lunchBreakMinutes = data.lunchBreakMinutes();
    }

    private static void validate(FrontData data) {
        Objects.requireNonNull(data, "data");
        if (data.endDate().isBefore(data.startDate())) {
            throw new BusinessRuleException("frente.fechas.invalidas");
        }
        if (data.openings() < 1 || data.openings() > MAX_OPENINGS) {
            throw new BusinessRuleException("frente.cupos.invalidos", MAX_OPENINGS);
        }
        if (data.offeredDailyWage().signum() <= 0) {
            throw new BusinessRuleException("frente.jornal.invalido");
        }
        if (data.workDays() == null || data.workDays().isEmpty()) {
            throw new BusinessRuleException("frente.dias.vacios");
        }
        if (data.workDays().contains(DayOfWeek.SUNDAY)) {
            throw new BusinessRuleException("frente.domingo.no.permitido");
        }
        if (!data.endTime().isAfter(data.startTime())) {
            throw new BusinessRuleException("frente.horario.invalido");
        }
        if (data.startTime().isBefore(DAYTIME_START) || data.endTime().isAfter(DAYTIME_END)) {
            throw new BusinessRuleException("frente.horario.no.diurno");
        }
        long shiftMinutes = Duration.between(data.startTime(), data.endTime()).toMinutes();
        if (data.lunchBreakMinutes() < 0 || data.lunchBreakMinutes() > MAX_LUNCH_MINUTES
                || data.lunchBreakMinutes() >= shiftMinutes) {
            throw new BusinessRuleException("frente.almuerzo.invalido", MAX_LUNCH_MINUTES);
        }
        long weekly = (shiftMinutes - data.lunchBreakMinutes()) * data.workDays().size();
        if (weekly > MAX_WEEKLY_MINUTES) {
            throw new BusinessRuleException("frente.horas.semanales.exceden",
                    formatHours(weekly), MAX_WEEKLY_MINUTES / 60);
        }
    }

    /** Minutos de trabajo programados por dia: fin menos inicio menos almuerzo. */
    public int scheduledMinutesPerDay() {
        return (int) Duration.between(startTime, endTime).toMinutes() - lunchBreakMinutes;
    }

    public int scheduledMinutesPerWeek() {
        return scheduledMinutesPerDay() * workDays.size();
    }

    /** Dia de trabajo programado que no es festivo. */
    public boolean isWorkingDay(LocalDate date) {
        return isScheduledDay(date) && !ColombianHolidays.isHoliday(date);
    }

    /** El dia de la semana esta en el horario del frente (sea o no festivo). */
    public boolean isScheduledDay(LocalDate date) {
        return workDays.contains(date.getDayOfWeek());
    }

    public boolean coversDate(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** Valor de una hora ordinaria: jornal dividido entre las horas programadas. */
    public BigDecimal hourlyRate(BigDecimal dailyWage) {
        return dailyWage.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(scheduledMinutesPerDay()), 0, RoundingMode.HALF_UP);
    }

    public long countActiveAssignments() {
        return assignments.stream().filter(a -> a.getStatus().isActive()).count();
    }

    public long countInProgress() {
        return assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS).count();
    }

    /** Cupos libres: cupos menos asignaciones invitadas o en curso. */
    public int availableOpenings() {
        return (int) Math.max(0, openings - countActiveAssignments());
    }

    void addAssignment(Assignment assignment) {
        assignments.add(assignment);
    }

    static String formatHours(long minutes) {
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
    }

    public ConstructionSite getSite() {
        return site;
    }

    public String getTask() {
        return task;
    }

    public Trade getRequiredTrade() {
        return requiredTrade;
    }

    public int getOpenings() {
        return openings;
    }

    public BigDecimal getOfferedDailyWage() {
        return offeredDailyWage;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /** Dias laborales en orden (lunes primero). */
    public Set<DayOfWeek> getWorkDays() {
        return Collections.unmodifiableSet(new TreeSet<>(workDays));
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getLunchBreakMinutes() {
        return lunchBreakMinutes;
    }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    /** Datos con los que se abre un frente. */
    public record FrontData(
            String task,
            Trade requiredTrade,
            int openings,
            BigDecimal offeredDailyWage,
            LocalDate startDate,
            LocalDate endDate,
            Set<DayOfWeek> workDays,
            LocalTime startTime,
            LocalTime endTime,
            int lunchBreakMinutes) {
    }
}
