package com.jornal.service;

import com.jornal.config.LaborProperties;
import com.jornal.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Piso legal del jornal. La hora ordinaria pagada no puede valer menos que la
 * hora del salario minimo (SMMLV / horas del mes). Como un frente puede tener
 * jornadas de 8 h o de 6 h, el jornal minimo depende de su horario.
 */
@Component
public class WagePolicy {

    private final LaborProperties properties;

    public WagePolicy(LaborProperties properties) {
        this.properties = properties;
    }

    public BigDecimal minimumHourlyRate() {
        return properties.minimumMonthlyWage()
                .divide(BigDecimal.valueOf(properties.monthlyHours()), 2, RoundingMode.HALF_UP);
    }

    /** Jornal minimo para un horario, redondeado hacia arriba al peso. */
    public BigDecimal minimumDailyWage(int scheduledMinutesPerDay) {
        return minimumHourlyRate()
                .multiply(BigDecimal.valueOf(scheduledMinutesPerDay))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.CEILING);
    }

    public void requireLegalWage(BigDecimal dailyWage, int scheduledMinutesPerDay, String code) {
        BigDecimal minimum = minimumDailyWage(scheduledMinutesPerDay);
        if (dailyWage.compareTo(minimum) < 0) {
            throw new BusinessRuleException(code, minimum);
        }
    }
}
