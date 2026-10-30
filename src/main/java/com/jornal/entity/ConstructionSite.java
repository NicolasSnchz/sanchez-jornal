package com.jornal.entity;

import com.jornal.exception.BusinessRuleException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Obra: el proyecto fisico de un maestro. Se compone de frentes de trabajo
 * (composicion: si la obra se elimina, sus frentes tambien).
 *
 * <p>Ciclo de vida: PLANNED -> IN_PROGRESS <-> SUSPENDED, IN_PROGRESS -> FINISHED.
 */
@Entity
@Table(name = "construction_sites")
public class ConstructionSite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "foreman_id", nullable = false)
    private Foreman foreman;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 150)
    private String address;

    @Column(nullable = false, length = 60)
    private String neighborhood;

    @Column(nullable = false, length = 60)
    private String city;

    /** Licencia de construccion; opcional porque las reparaciones menores no la exigen. */
    @Column(name = "building_permit", length = 40)
    private String buildingPermit;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "estimated_end_date", nullable = false)
    private LocalDate estimatedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteStatus status = SiteStatus.PLANNED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate ASC, task ASC")
    private List<WorkFront> fronts = new ArrayList<>();

    protected ConstructionSite() {
        // requerido por JPA
    }

    public ConstructionSite(Foreman foreman, SiteData data, LocalDateTime createdAt) {
        this.foreman = Objects.requireNonNull(foreman, "foreman");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        apply(data);
    }

    /** Actualiza los datos descriptivos. Las fechas no pueden dejar frentes por fuera. */
    public void update(SiteData data) {
        if (status == SiteStatus.FINISHED) {
            throw new BusinessRuleException("obra.finalizada.no.editable");
        }
        for (WorkFront front : fronts) {
            if (front.getStartDate().isBefore(data.startDate()) || front.getEndDate().isAfter(data.estimatedEndDate())) {
                throw new BusinessRuleException("obra.fechas.excluyen.frentes", front.getTask());
            }
        }
        apply(data);
    }

    private void apply(SiteData data) {
        Objects.requireNonNull(data, "data");
        if (data.estimatedEndDate().isBefore(data.startDate())) {
            throw new BusinessRuleException("obra.fechas.invalidas");
        }
        this.name = data.name().trim();
        this.description = blankToNull(data.description());
        this.address = data.address().trim();
        this.neighborhood = data.neighborhood().trim();
        this.city = data.city().trim();
        this.buildingPermit = blankToNull(data.buildingPermit());
        this.startDate = data.startDate();
        this.estimatedEndDate = data.estimatedEndDate();
    }

    public void start() {
        requireStatus(SiteStatus.PLANNED, "obra.iniciar.estado.invalido");
        status = SiteStatus.IN_PROGRESS;
    }

    public void suspend() {
        requireStatus(SiteStatus.IN_PROGRESS, "obra.suspender.estado.invalido");
        status = SiteStatus.SUSPENDED;
    }

    public void resume() {
        requireStatus(SiteStatus.SUSPENDED, "obra.reanudar.estado.invalido");
        status = SiteStatus.IN_PROGRESS;
    }

    /** Solo se finaliza cuando nadie sigue trabajando ni tiene una invitacion abierta. */
    public void finish() {
        requireStatus(SiteStatus.IN_PROGRESS, "obra.finalizar.estado.invalido");
        long active = countActiveAssignments();
        if (active > 0) {
            throw new BusinessRuleException("obra.finalizar.asignaciones.activas", active);
        }
        status = SiteStatus.FINISHED;
    }

    /** Abre un frente; sus fechas deben caber dentro de las de la obra. */
    public WorkFront openFront(WorkFront.FrontData data) {
        if (status == SiteStatus.FINISHED) {
            throw new BusinessRuleException("frente.obra.finalizada");
        }
        if (data.startDate().isBefore(startDate) || data.endDate().isAfter(estimatedEndDate)) {
            throw new BusinessRuleException("frente.fechas.fuera.de.obra", startDate, estimatedEndDate);
        }
        WorkFront front = new WorkFront(this, data);
        fronts.add(front);
        return front;
    }

    /** Una obra en curso esta retrasada si ya paso su fecha estimada de fin. */
    public boolean isDelayed(LocalDate today) {
        return status == SiteStatus.IN_PROGRESS && today.isAfter(estimatedEndDate);
    }

    public boolean isOwnedBy(UUID foremanId) {
        return foreman.getId().equals(foremanId);
    }

    public long countActiveAssignments() {
        return fronts.stream().mapToLong(WorkFront::countActiveAssignments).sum();
    }

    public long countWorkersInProgress() {
        return fronts.stream().mapToLong(WorkFront::countInProgress).sum();
    }

    /** Costo de mano de obra: todo lo devengado en sus frentes (confirmado o no). */
    public BigDecimal calculateLaborCost() {
        return fronts.stream()
                .flatMap(front -> front.getAssignments().stream())
                .map(Assignment::calculateEarnedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireStatus(SiteStatus expected, String code) {
        if (status != expected) {
            throw new BusinessRuleException(code);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Foreman getForeman() {
        return foreman;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getBuildingPermit() {
        return buildingPermit;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEstimatedEndDate() {
        return estimatedEndDate;
    }

    public SiteStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<WorkFront> getFronts() {
        return Collections.unmodifiableList(fronts);
    }

    /** Datos editables de una obra. */
    public record SiteData(
            String name,
            String description,
            String address,
            String neighborhood,
            String city,
            String buildingPermit,
            LocalDate startDate,
            LocalDate estimatedEndDate) {
    }
}
