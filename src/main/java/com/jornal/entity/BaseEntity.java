package com.jornal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;

/**
 * Identidad comun de todas las entidades.
 * El id es un UUID asignado al crear el objeto: no es adivinable (mitiga IDOR,
 * OWASP A01) y permite un equals/hashCode estable antes de persistir.
 * El campo version activa el bloqueo optimista para operaciones concurrentes.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Version
    private Long version;

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // Se usa el getter (no el campo) para que funcione con proxies de Hibernate
        if (!(other instanceof BaseEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
