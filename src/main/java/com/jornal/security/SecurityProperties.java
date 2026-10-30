package com.jornal.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametros de seguridad leidos de application.properties.
 * El secreto JWT llega por la variable de entorno JWT_SECRET; nunca se escribe en el codigo.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotBlank String jwtSecret,
        Duration jwtExpiration,
        @Min(1) int maxLoginAttempts,
        Duration lockDuration,
        @Min(4) int bcryptStrength) {
}
