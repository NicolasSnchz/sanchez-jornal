package com.jornal.entity;

import com.jornal.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Jornada: un dia trabajado dentro de una asignacion (composicion: no existe sin
 * ella). Guarda lo que paso (entrada, salida, descanso) y nunca un valor en
 * dinero: el valor se calcula con el jornal pactado y el horario del frente.
 */
@Entity
@Table(name = "work_days")
public class WorkDay extends BaseEntity {

    /** Recargo de la hora extra diurna: 25 % (articulo 168 del Codigo Sustantivo del Trabajo). */
    public static final int OVERTIME_SURCHARGE_PERCENT = 25;
    /** Las horas extra no pueden superar dos por dia (articulo 22 de la Ley 50 de 1990). */
    public static final int MAX_OVERTIME_MINUTES = 120;
    public static final int MAX_BREAK_MINUTES = 180;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime;

    @Column(name = "check_out_time", nullable = false)
    private LocalTime checkOutTime;

    @Column(name = "break_minutes", nullable = false)
    private int breakMinutes;

    @Column(name = "task_performed", nullable = false, length = 200)
    private String taskPerformed;

    @Column(length = 500)
    private String notes;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    /** Se conserva despues de corregir la jornada para dejar constancia del reclamo. */
    @Column(name = "dispute_reason", length = 300)
    private String disputeReason;

    @Column(name = "corrected_at")
    private LocalDateTime correctedAt;

    protected WorkDay() {
        // requerido por JPA
    }

    WorkDay(Assignment assignment, LocalDate date, TimeRecord record, LocalDateTime registeredAt) {
        this.assignment = Objects.requireNonNull(assignment, "assignment");
        this.date = Objects.requireNonNull(date, "date");
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
        apply(record);
    }

    /** Valida un registro de horas contra el horario programado del frente. */
    static void validate(TimeRecord record, int scheduledMinutes) {
        Objects.requireNonNull(record, "record");
        if (record.checkInTime().isBefore(WorkFront.DAYTIME_START)
                || record.checkOutTime().isAfter(WorkFront.DAYTIME_END)) {
            throw new BusinessRuleException("jornada.horario.no.diurno");
        }
        if (!record.checkOutTime().isAfter(record.checkInTime())) {
            throw new BusinessRuleException("jornada.horario.invalido");
        }
        long presence = Duration.between(record.checkInTime(), record.checkOutTime()).toMinutes();
        if (record.breakMinutes() < 0 || record.breakMinutes() > MAX_BREAK_MINUTES
                || record.breakMinutes() >= presence) {
            throw new BusinessRuleException("jornada.descanso.invalido");
        }
        long worked = presence - record.breakMinutes();
        if (worked - scheduledMinutes > MAX_OVERTIME_MINUTES) {
            throw new BusinessRuleException("jornada.horas.extra.exceden", MAX_OVERTIME_MINUTES / 60);
        }
    }

    private void apply(TimeRecord record) {
        this.checkInTime = record.checkInTime();
        this.checkOutTime = record.checkOutTime();
        this.breakMinutes = record.breakMinutes();
        this.taskPerformed = record.taskPerformed().trim();
        this.notes = record.notes() == null || record.notes().isBlank() ? null : record.notes().trim();
    }

    public WorkDayStatus getStatus() {
        if (confirmedAt != null) {
            return WorkDayStatus.CONFIRMED;
        }
        return disputedAt != null ? WorkDayStatus.DISPUTED : WorkDayStatus.PENDING;
    }

    /** Minutos efectivamente trabajados: salida menos entrada menos descanso. */
    public int workedMinutes() {
        return (int) Duration.between(checkInTime, checkOutTime).toMinutes() - breakMinutes;
    }

    public int overtimeMinutes(int scheduledMinutes) {
        return Math.max(0, workedMinutes() - scheduledMinutes);
    }

    /**
     * Fraccion de jornal: horas ordinarias trabajadas sobre horas programadas,
     * con maximo 1. Media jornada = 0,5.
     */
    public BigDecimal fraction(int scheduledMinutes) {
        int ordinary = Math.min(workedMinutes(), scheduledMinutes);
        return BigDecimal.valueOf(ordinary).divide(BigDecimal.valueOf(scheduledMinutes), 2, RoundingMode.HALF_UP);
    }

    /**
     * Valor en pesos: (minutos ordinarios + minutos extra x 1,25) x jornal / minutos programados.
     * Se hace una sola division y se redondea a pesos enteros al final, para no
     * acumular errores de redondeo intermedios.
     */
    public BigDecimal value(BigDecimal dailyWage, int scheduledMinutes) {
        int worked = workedMinutes();
        int ordinary = Math.min(worked, scheduledMinutes);
        int overtime = Math.max(0, worked - scheduledMinutes);
        BigDecimal weightedMinutes = BigDecimal.valueOf(ordinary * 100L
                + overtime * (100L + OVERTIME_SURCHARGE_PERCENT));
        return dailyWage.multiply(weightedMinutes)
                .divide(BigDecimal.valueOf(scheduledMinutes * 100L), 0, RoundingMode.HALF_UP);
    }

    /** El trabajador confirma que el registro es correcto. */
    public void confirm(LocalDateTime now) {
        if (getStatus() != WorkDayStatus.PENDING) {
            throw new BusinessRuleException("jornada.confirmar.estado.invalido");
        }
        this.confirmedAt = now;
    }

    /** El trabajador objeta el registro; mientras siga objetada no se puede liquidar. */
    public void dispute(String reason, LocalDateTime now) {
        if (getStatus() != WorkDayStatus.PENDING) {
            throw new BusinessRuleException("jornada.objetar.estado.invalido");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("jornada.objecion.motivo.requerido");
        }
        this.disputeReason = reason.trim();
        this.disputedAt = now;
    }

    /** El maestro corrige una jornada no confirmada; vuelve a quedar por confirmar. */
    void correct(TimeRecord record, int scheduledMinutes, LocalDateTime now) {
        if (getStatus() == WorkDayStatus.CONFIRMED) {
            throw new BusinessRuleException("jornada.corregir.confirmada");
        }
        validate(record, scheduledMinutes);
        apply(record);
        this.disputedAt = null;
        this.correctedAt = now;
    }

    public boolean wasCorrectedAfterDispute() {
        return correctedAt != null && disputeReason != null;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public int getBreakMinutes() {
        return breakMinutes;
    }

    public String getTaskPerformed() {
        return taskPerformed;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getDisputedAt() {
        return disputedAt;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public LocalDateTime getCorrectedAt() {
        return correctedAt;
    }

    /** Registro de horas de un dia, tal como lo anota el maestro. */
    public record TimeRecord(
            LocalTime checkInTime,
            LocalTime checkOutTime,
            int breakMinutes,
            String taskPerformed,
            String notes) {
    }
}
