package com.jornal.entity;

import com.jornal.exception.BusinessRuleException;
import com.jornal.util.ColombianHolidays;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Asignacion: une a un trabajador con un frente y guarda lo que pertenece a esa
 * pareja (jornal pactado, fechas, estado y calificaciones). Resuelve la relacion
 * muchos a muchos entre Worker y WorkFront; es una clase propia y no una clase
 * de asociacion porque un trabajador puede volver al mismo frente otra vez.
 *
 * <p>Ciclo de vida: INVITED -> IN_PROGRESS -> COMPLETED; INVITED -> REJECTED;
 * INVITED o IN_PROGRESS sin jornadas -> CANCELLED.
 */
@Entity
@Table(name = "assignments")
public class Assignment extends BaseEntity {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "front_id", nullable = false)
    private WorkFront front;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.INVITED;

    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    /** Jornal pactado: puede diferir del ofrecido y queda fijo para esta asignacion. */
    @Column(name = "daily_wage", nullable = false, precision = 12, scale = 0)
    private BigDecimal dailyWage;

    /** Calificacion que el maestro da al trabajador al finalizar. */
    @Column(name = "worker_rating")
    private Integer workerRating;

    @Column(name = "worker_rating_comment", length = 300)
    private String workerRatingComment;

    /** Calificacion que el trabajador da al maestro al finalizar. */
    @Column(name = "foreman_rating")
    private Integer foremanRating;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC")
    private List<WorkDay> workDays = new ArrayList<>();

    @OneToMany(mappedBy = "assignment")
    @OrderBy("paidAt ASC")
    private List<Payment> payments = new ArrayList<>();

    protected Assignment() {
        // requerido por JPA
    }

    private Assignment(Worker worker, WorkFront front, BigDecimal dailyWage, LocalDate startDate, LocalDateTime now) {
        this.worker = worker;
        this.front = front;
        this.dailyWage = dailyWage.setScale(0, RoundingMode.HALF_UP);
        this.startDate = startDate;
        this.invitedAt = now;
    }

    /**
     * Crea una invitacion. La elegibilidad del trabajador la revisa el servicio
     * (necesita consultar otras asignaciones); aqui se validan las reglas del frente.
     */
    public static Assignment invite(WorkFront front, Worker worker, BigDecimal dailyWage,
                                    LocalDate startDate, LocalDateTime now) {
        Objects.requireNonNull(front, "front");
        Objects.requireNonNull(worker, "worker");
        SiteStatus siteStatus = front.getSite().getStatus();
        if (siteStatus != SiteStatus.PLANNED && siteStatus != SiteStatus.IN_PROGRESS) {
            throw new BusinessRuleException("invitacion.obra.no.admite");
        }
        if (front.availableOpenings() == 0) {
            throw new BusinessRuleException("invitacion.sin.cupos");
        }
        if (dailyWage == null || dailyWage.signum() <= 0) {
            throw new BusinessRuleException("invitacion.jornal.invalido");
        }
        if (startDate.isBefore(now.toLocalDate()) || !front.coversDate(startDate)) {
            throw new BusinessRuleException("invitacion.inicio.invalido", front.getStartDate(), front.getEndDate());
        }
        Assignment assignment = new Assignment(worker, front, dailyWage, startDate, now);
        front.addAssignment(assignment);
        return assignment;
    }

    // ---------------------------------------------------------------- ciclo de vida

    public void accept(LocalDateTime now) {
        requireStatus(AssignmentStatus.INVITED, "asignacion.responder.estado.invalido");
        status = AssignmentStatus.IN_PROGRESS;
        respondedAt = now;
    }

    public void reject(LocalDateTime now) {
        requireStatus(AssignmentStatus.INVITED, "asignacion.responder.estado.invalido");
        status = AssignmentStatus.REJECTED;
        respondedAt = now;
    }

    /** Se cancela una invitacion abierta o una asignacion en la que aun no se ha trabajado. */
    public void cancel(String reason, LocalDateTime now) {
        if (!status.isActive()) {
            throw new BusinessRuleException("asignacion.cancelar.estado.invalido");
        }
        if (!workDays.isEmpty()) {
            throw new BusinessRuleException("asignacion.cancelar.con.jornadas");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("asignacion.cancelar.motivo.requerido");
        }
        cancellationReason = reason.trim();
        status = AssignmentStatus.CANCELLED;
        if (respondedAt == null) {
            respondedAt = now;
        }
    }

    /**
     * Finaliza la asignacion: no puede quedar ninguna jornada sin confirmar ni
     * saldo pendiente. El maestro califica al trabajador en este momento.
     */
    public void complete(int rating, String comment, LocalDate today) {
        requireStatus(AssignmentStatus.IN_PROGRESS, "asignacion.finalizar.estado.invalido");
        if (workDays.isEmpty()) {
            throw new BusinessRuleException("asignacion.finalizar.sin.jornadas");
        }
        if (countDays(day -> day.getStatus() != WorkDayStatus.CONFIRMED) > 0) {
            throw new BusinessRuleException("asignacion.finalizar.jornadas.sin.confirmar");
        }
        if (calculateBalance().signum() != 0) {
            throw new BusinessRuleException("asignacion.finalizar.saldo.pendiente", calculateBalance());
        }
        requireValidRating(rating);
        workerRating = rating;
        workerRatingComment = comment == null || comment.isBlank() ? null : comment.trim();
        actualEndDate = lastWorkDayDate().orElse(today);
        status = AssignmentStatus.COMPLETED;
    }

    /** El trabajador califica al maestro una sola vez, despues de finalizar. */
    public void rateForeman(int rating) {
        requireStatus(AssignmentStatus.COMPLETED, "asignacion.calificar.estado.invalido");
        if (foremanRating != null) {
            throw new BusinessRuleException("asignacion.calificar.repetida");
        }
        requireValidRating(rating);
        foremanRating = rating;
    }

    // ---------------------------------------------------------------- jornadas

    /** Registra un dia trabajado validando calendario, horario y periodos liquidados. */
    public WorkDay registerWorkDay(LocalDate date, WorkDay.TimeRecord record, LocalDate today, LocalDateTime now) {
        requireStatus(AssignmentStatus.IN_PROGRESS, "jornada.asignacion.no.en.curso");
        if (front.getSite().getStatus() != SiteStatus.IN_PROGRESS) {
            throw new BusinessRuleException("jornada.obra.no.en.curso");
        }
        if (hasEffectiveFinalSettlement()) {
            throw new BusinessRuleException("jornada.asignacion.liquidada");
        }
        if (date.isAfter(today)) {
            throw new BusinessRuleException("jornada.fecha.futura");
        }
        if (date.isBefore(startDate) || date.isAfter(front.getEndDate())) {
            throw new BusinessRuleException("jornada.fecha.fuera.de.periodo", startDate, front.getEndDate());
        }
        Optional<String> holiday = ColombianHolidays.nameOf(date);
        if (holiday.isPresent()) {
            throw new BusinessRuleException("jornada.festivo", date, holiday.get());
        }
        if (!front.isWorkingDay(date)) {
            throw new BusinessRuleException("jornada.dia.no.laboral");
        }
        if (findWorkDayOn(date).isPresent()) {
            throw new BusinessRuleException("jornada.duplicada");
        }
        requireNotSettled(date);
        WorkDay.validate(record, front.scheduledMinutesPerDay());
        WorkDay day = new WorkDay(this, date, record, now);
        workDays.add(day);
        return day;
    }

    /** El maestro corrige una jornada por confirmar u objetada que no este liquidada. */
    public void correctWorkDay(WorkDay day, WorkDay.TimeRecord record, LocalDateTime now) {
        if (!workDays.contains(day)) {
            throw new BusinessRuleException("jornada.no.pertenece");
        }
        requireNotSettled(day.getDate());
        day.correct(record, front.scheduledMinutesPerDay(), now);
    }

    private void requireNotSettled(LocalDate date) {
        Optional<LocalDate> settledUntil = lastSettledDate();
        if (settledUntil.isPresent() && !date.isAfter(settledUntil.get())) {
            throw new BusinessRuleException("jornada.periodo.liquidado", settledUntil.get());
        }
    }

    public Optional<WorkDay> findWorkDayOn(LocalDate date) {
        return workDays.stream().filter(day -> day.getDate().equals(date)).findFirst();
    }

    public Optional<LocalDate> lastWorkDayDate() {
        return workDays.stream().map(WorkDay::getDate).max(Comparator.naturalOrder());
    }

    // ---------------------------------------------------------------- dinero

    public BigDecimal valueOf(WorkDay day) {
        return day.value(dailyWage, front.scheduledMinutesPerDay());
    }

    /** Devengado confirmado: base del saldo, porque el trabajador ya lo acepto. */
    public BigDecimal calculateEarnedConfirmed() {
        return sumDays(day -> day.getStatus() == WorkDayStatus.CONFIRMED);
    }

    /** Devengado por confirmar u objetado: se muestra aparte y aun no se puede pagar. */
    public BigDecimal calculateEarnedUnconfirmed() {
        return sumDays(day -> day.getStatus() != WorkDayStatus.CONFIRMED);
    }

    public BigDecimal calculateEarnedTotal() {
        return sumDays(day -> true);
    }

    /** Devengado confirmado hasta una fecha (inclusive). */
    public BigDecimal calculateEarnedConfirmedUntil(LocalDate until) {
        return sumDays(day -> day.getStatus() == WorkDayStatus.CONFIRMED && !day.getDate().isAfter(until));
    }

    /** Total pagado: pagos no anulados (confirmados o por confirmar). */
    public BigDecimal calculatePaidTotal() {
        return payments.stream()
                .filter(Payment::isEffective)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Saldo = devengado confirmado - pagado. Las reglas de pago impiden que sea negativo. */
    public BigDecimal calculateBalance() {
        return calculateEarnedConfirmed().subtract(calculatePaidTotal());
    }

    /** Ultimo dia cubierto por una liquidacion vigente; antes de el no se edita nada. */
    public Optional<LocalDate> lastSettledDate() {
        return payments.stream()
                .filter(Payment::isEffective)
                .filter(Settlement.class::isInstance)
                .map(payment -> ((Settlement) payment).getPeriodEnd())
                .max(Comparator.naturalOrder());
    }

    /** Primer dia que aun no esta liquidado: inicio del siguiente periodo. */
    public LocalDate nextUnsettledDate() {
        return lastSettledDate().map(date -> date.plusDays(1)).orElse(startDate);
    }

    public boolean hasEffectiveFinalSettlement() {
        return payments.stream()
                .filter(Payment::isEffective)
                .anyMatch(payment -> payment instanceof Settlement settlement && settlement.isFinalSettlement());
    }

    /** Ultima liquidacion vigente: la unica que se puede anular sin romper los periodos. */
    public Optional<Settlement> latestEffectiveSettlement() {
        return payments.stream()
                .filter(Payment::isEffective)
                .filter(Settlement.class::isInstance)
                .map(Settlement.class::cast)
                .max(Comparator.comparing(Settlement::getPeriodEnd));
    }

    public long countDays(Predicate<WorkDay> condition) {
        return workDays.stream().filter(condition).count();
    }

    public long countUnconfirmedDaysUntil(LocalDate until) {
        return countDays(day -> day.getStatus() != WorkDayStatus.CONFIRMED && !day.getDate().isAfter(until));
    }

    void addPayment(Payment payment) {
        payments.add(payment);
    }

    private BigDecimal sumDays(Predicate<WorkDay> condition) {
        return workDays.stream()
                .filter(condition)
                .map(this::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------------------------------------------------------------- utilidades

    public boolean belongsToWorker(UUID workerId) {
        return worker.getId().equals(workerId);
    }

    public boolean belongsToForeman(UUID foremanId) {
        return front.getSite().isOwnedBy(foremanId);
    }

    /** Ultimo dia en que la asignacion ocupa la agenda del trabajador. */
    public LocalDate plannedEndDate() {
        return actualEndDate != null ? actualEndDate : front.getEndDate();
    }

    private void requireStatus(AssignmentStatus expected, String code) {
        if (status != expected) {
            throw new BusinessRuleException(code);
        }
    }

    private static void requireValidRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessRuleException("calificacion.fuera.de.rango", MIN_RATING, MAX_RATING);
        }
    }

    public Worker getWorker() {
        return worker;
    }

    public WorkFront getFront() {
        return front;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public BigDecimal getDailyWage() {
        return dailyWage;
    }

    public Integer getWorkerRating() {
        return workerRating;
    }

    public String getWorkerRatingComment() {
        return workerRatingComment;
    }

    public Integer getForemanRating() {
        return foremanRating;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public List<WorkDay> getWorkDays() {
        return Collections.unmodifiableList(workDays);
    }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }
}
