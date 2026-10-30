package com.jornal.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado de evaluar un pago antes de registrarlo: el monto que se pagaria y,
 * para las liquidaciones, el periodo y el desglose (devengado menos descuentos).
 */
public record PaymentQuote(
        BigDecimal amount,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal earnedInPeriod,
        BigDecimal discounted) {

    public static PaymentQuote ofAmount(BigDecimal amount) {
        return new PaymentQuote(amount, null, null, null, null);
    }
}
