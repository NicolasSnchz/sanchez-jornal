package com.jornal.strategy;

import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos que el maestro envia para registrar un pago. Segun el tipo se usan unos
 * campos u otros: el anticipo usa monto y motivo; la liquidacion usa el fin del
 * periodo y calcula el monto ella misma.
 */
public record PaymentCommand(
        PaymentType type,
        BigDecimal amount,
        PaymentMethod method,
        String transactionReference,
        String reason,
        boolean requestedByWorker,
        LocalDate periodEnd) {
}
