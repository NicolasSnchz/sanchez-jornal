package com.jornal.security;

/**
 * Reglas de autorizacion por metodo (se usan con {@code @PreAuthorize}).
 *
 * <p>No basta con el rol: el id que recibe el servicio debe ser el del usuario
 * autenticado. Asi, aunque un controlador pasara por error otro id, el servicio
 * se niega (defensa en profundidad contra IDOR, OWASP A01). Ademas, cada
 * consulta filtra por ese id, de modo que un recurso ajeno responde 404.
 */
public final class Policies {

    public static final String FOREMAN_SELF = "hasRole('FOREMAN') and #foremanId == principal.id()";
    public static final String WORKER_SELF = "hasRole('WORKER') and #workerId == principal.id()";
    public static final String SELF = "isAuthenticated() and #userId == principal.id()";

    private Policies() {
    }
}
