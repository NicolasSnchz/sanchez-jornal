package com.jornal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Locale;
import java.util.Objects;

/**
 * Persona registrada en Jornal. Es abstracta: todo usuario es maestro de obra
 * o trabajador, y de esa subclase se deriva su rol de seguridad.
 * Ademas de los datos personales guarda el estado de la cuenta: contrasena
 * cifrada con BCrypt e intentos fallidos para el bloqueo temporal (OWASP A07).
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseEntity {

    public static final int ADULT_AGE = 18;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
        // requerido por JPA
    }

    protected User(PersonalData data, String passwordHash, LocalDateTime createdAt) {
        Objects.requireNonNull(data, "data");
        this.documentType = Objects.requireNonNull(data.documentType());
        this.documentNumber = data.documentNumber().trim();
        this.firstName = data.firstName().trim();
        this.lastName = data.lastName().trim();
        this.birthDate = Objects.requireNonNull(data.birthDate());
        this.gender = Objects.requireNonNull(data.gender());
        this.phone = data.phone().trim();
        this.email = normalizeEmail(data.email());
        this.city = data.city().trim();
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Correo en minusculas y sin espacios: es el nombre de usuario para iniciar sesion. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public abstract Role getRole();

    public int calculateAge(LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    /** La construccion esta prohibida para menores de edad en Colombia. */
    public boolean isAdult(LocalDate today) {
        return calculateAge(today) >= ADULT_AGE;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Iniciales para el avatar de la interfaz. */
    public String getInitials() {
        return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Registra un intento fallido. Al llegar al maximo, la cuenta queda bloqueada
     * durante el tiempo indicado y el contador vuelve a cero.
     *
     * @return true si este intento provoco el bloqueo
     */
    public boolean registerFailedLogin(LocalDateTime now, int maxAttempts, Duration lockDuration) {
        if (isLocked(now)) {
            return false;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    public void registerSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void updateContact(String phone, String city) {
        this.phone = phone.trim();
        this.city = city.trim();
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getCity() {
        return city;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Datos personales comunes a maestros y trabajadores. */
    public record PersonalData(
            DocumentType documentType,
            String documentNumber,
            String firstName,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String phone,
            String email,
            String city) {
    }
}
