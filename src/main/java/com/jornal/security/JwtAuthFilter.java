package com.jornal.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida el JWT de cada peticion a /api/** y llena el SecurityContext.
 * Ademas del token revisa en la base de datos que la cuenta siga activa y sin
 * bloqueo: una cuenta desactivada pierde el acceso de inmediato, sin esperar a
 * que expire el token.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Atributo que lee el punto de entrada para explicar por que se respondio 401. */
    public static final String ERROR_ATTRIBUTE = "jornal.jwt.error";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JornalUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, JornalUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            UUID userId = jwtService.validateAndGetUserId(token);
            JornalUserDetails user = userDetailsService.loadById(userId);
            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                request.setAttribute(ERROR_ATTRIBUTE, "api.token.cuenta.no.disponible");
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.debug("Token rechazado: {}", e.getClass().getSimpleName());
            request.setAttribute(ERROR_ATTRIBUTE, "api.token.invalido");
        }
    }
}
