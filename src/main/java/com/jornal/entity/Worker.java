package com.jornal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Trabajador por dias: recibe invitaciones, confirma sus jornadas y cobra. */
@Entity
@Table(name = "workers")
@PrimaryKeyJoinColumn(name = "user_id")
public class Worker extends User {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Trade trade;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(nullable = false, length = 60)
    private String neighborhood;

    @Column(name = "has_own_tools", nullable = false)
    private boolean hasOwnTools;

    /** Vigencia de la afiliacion a riesgos laborales: es una fecha porque vence. */
    @Column(name = "arl_valid_until", nullable = false)
    private LocalDate arlValidUntil;

    /** Preferencia manual: si es false no aparece en las busquedas de los maestros. */
    @Column(name = "accepting_offers", nullable = false)
    private boolean acceptingOffers = true;

    @Column(name = "emergency_phone", nullable = false, length = 20)
    private String emergencyPhone;

    protected Worker() {
        // requerido por JPA
    }

    public Worker(PersonalData data, WorkProfile profile, String passwordHash, LocalDateTime createdAt) {
        super(data, passwordHash, createdAt);
        applyProfile(profile);
    }

    @Override
    public Role getRole() {
        return Role.WORKER;
    }

    public void updateWorkProfile(WorkProfile profile) {
        applyProfile(profile);
    }

    public void changeAvailability(boolean acceptingOffers) {
        this.acceptingOffers = acceptingOffers;
    }

    public boolean hasArlValidThrough(LocalDate date) {
        return !arlValidUntil.isBefore(date);
    }

    /**
     * Motivos por los que el trabajador no puede tomar un frente, evaluando solo
     * sus propios datos. El cruce de fechas con otras asignaciones necesita la
     * base de datos y lo revisa el servicio.
     *
     * @return codigos de mensaje; lista vacia si es apto
     */
    public List<String> eligibilityIssues(WorkFront front, LocalDate today) {
        Objects.requireNonNull(front, "front");
        List<String> issues = new ArrayList<>();
        if (!isActive()) {
            issues.add("elegibilidad.cuenta.inactiva");
        }
        if (!isAdult(today)) {
            issues.add("elegibilidad.menor.de.edad");
        }
        if (!acceptingOffers) {
            issues.add("elegibilidad.no.recibe.ofertas");
        }
        if (trade != front.getRequiredTrade()) {
            issues.add("elegibilidad.oficio.distinto");
        }
        if (!hasArlValidThrough(front.getEndDate())) {
            issues.add("elegibilidad.arl.vence.antes");
        }
        return issues;
    }

    private void applyProfile(WorkProfile profile) {
        Objects.requireNonNull(profile, "profile");
        this.trade = Objects.requireNonNull(profile.trade());
        this.yearsOfExperience = profile.yearsOfExperience();
        this.neighborhood = profile.neighborhood().trim();
        this.hasOwnTools = profile.hasOwnTools();
        this.arlValidUntil = Objects.requireNonNull(profile.arlValidUntil());
        this.emergencyPhone = profile.emergencyPhone().trim();
    }

    public Trade getTrade() {
        return trade;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public boolean isHasOwnTools() {
        return hasOwnTools;
    }

    public LocalDate getArlValidUntil() {
        return arlValidUntil;
    }

    public boolean isAcceptingOffers() {
        return acceptingOffers;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    /** Datos de trabajo propios del trabajador. */
    public record WorkProfile(
            Trade trade,
            int yearsOfExperience,
            String neighborhood,
            boolean hasOwnTools,
            LocalDate arlValidUntil,
            String emergencyPhone) {
    }
}
