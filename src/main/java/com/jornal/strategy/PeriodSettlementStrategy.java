package com.jornal.strategy;

import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentType;
import com.jornal.entity.Settlement;
import com.jornal.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Liquidacion de un periodo. El inicio no se elige: es el dia siguiente a la
 * ultima liquidacion, asi los periodos quedan seguidos y sin huecos. El monto
 * tampoco se escribe a mano: es lo devengado y confirmado hasta el fin del
 * periodo menos todo lo ya pagado (que incluye los anticipos).
 */
@Component
public class PeriodSettlementStrategy implements PaymentStrategy {

    @Override
    public PaymentType type() {
        return PaymentType.SETTLEMENT;
    }

    @Override
    public PaymentQuote quote(Assignment assignment, PaymentCommand command, LocalDate today) {
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new BusinessRuleException("pago.asignacion.no.en.curso");
        }
        LocalDate periodEnd = command.periodEnd();
        if (periodEnd == null) {
            throw new BusinessRuleException("pago.liquidacion.fin.requerido");
        }
        LocalDate periodStart = assignment.nextUnsettledDate();
        if (periodEnd.isBefore(periodStart)) {
            throw new BusinessRuleException("pago.liquidacion.periodo.invalido", periodStart);
        }
        if (periodEnd.isAfter(today)) {
            throw new BusinessRuleException("pago.liquidacion.periodo.futuro");
        }
        long unconfirmed = assignment.countUnconfirmedDaysUntil(periodEnd);
        if (unconfirmed > 0) {
            throw new BusinessRuleException("pago.liquidacion.jornadas.sin.confirmar", unconfirmed);
        }
        BigDecimal amount = assignment.calculateEarnedConfirmedUntil(periodEnd)
                .subtract(assignment.calculatePaidTotal());
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("pago.liquidacion.sin.saldo");
        }
        BigDecimal earnedInPeriod = assignment.calculateEarnedConfirmedUntil(periodEnd)
                .subtract(assignment.calculateEarnedConfirmedUntil(periodStart.minusDays(1)));
        return new PaymentQuote(amount, periodStart, periodEnd, earnedInPeriod, earnedInPeriod.subtract(amount));
    }

    @Override
    public Payment create(Assignment assignment, PaymentCommand command, String receiptNumber, LocalDateTime now) {
        PaymentQuote quote = quote(assignment, command, now.toLocalDate());
        return new Settlement(assignment, receiptNumber, quote.amount(), command.method(),
                command.transactionReference(), now, quote.periodStart(), quote.periodEnd(), false);
    }
}
