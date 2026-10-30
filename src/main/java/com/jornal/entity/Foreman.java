package com.jornal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Maestro de obra: registra obras, abre frentes y contrata trabajadores. */
@Entity
@Table(name = "foremen")
@PrimaryKeyJoinColumn(name = "user_id")
public class Foreman extends User {

    /** Opcional: muchos maestros trabajan como persona natural. */
    @Column(name = "company_name", length = 120)
    private String companyName;

    protected Foreman() {
        // requerido por JPA
    }

    public Foreman(PersonalData data, String companyName, String passwordHash, LocalDateTime createdAt) {
        super(data, passwordHash, createdAt);
        this.companyName = blankToNull(companyName);
    }

    @Override
    public Role getRole() {
        return Role.FOREMAN;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void updateCompanyName(String companyName) {
        this.companyName = blankToNull(companyName);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
