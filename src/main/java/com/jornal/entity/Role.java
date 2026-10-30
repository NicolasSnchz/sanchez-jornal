package com.jornal.entity;

/**
 * Rol de seguridad. No se guarda como columna: se deriva del tipo de usuario
 * (Foreman o Worker), por eso nunca puede quedar inconsistente con los datos.
 */
public enum Role {
    FOREMAN, WORKER;

    /** Nombre de autoridad que espera Spring Security (prefijo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
