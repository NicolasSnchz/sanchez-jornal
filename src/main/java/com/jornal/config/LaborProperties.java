package com.jornal.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametros laborales que cambian por ley y no por codigo: se ajustan en
 * application.properties cuando sale el decreto del nuevo ano.
 *
 * @param minimumMonthlyWage salario minimo mensual legal vigente (SMMLV)
 * @param monthlyHours       horas ordinarias del mes: jornada semanal / 6 x 30
 *                           (42 h desde julio de 2026, Ley 2101 de 2021, da 210)
 */
@Validated
@ConfigurationProperties(prefix = "app.labor")
public record LaborProperties(
        @NotNull @Positive BigDecimal minimumMonthlyWage,
        @Positive int monthlyHours) {
}
