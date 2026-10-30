package com.jornal.strategy;

import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentType;
import com.jornal.entity.Settlement;
import com.jornal.entity.WorkDayStatus;
import com.jornal.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Liquidacion final: paga todo el saldo y deja la asignacion lista para
 * finalizar. Exige que todas las jornadas esten confirmadas, porque despues de
 * ella ya no se pueden registrar ni corregir jornadas.
 */
@Component
public class FinalSettlementStrategy implements PaymentStrategy {

    @Override
    public PaymentType type() {
        return PaymentType.FINAL_SETTLEMENT;
    }

    @Override
    public PaymentQuote quote(Assignment assignment, PaymentCommand command, LocalDate today) {
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new BusinessRuleException("pago.asignacion.no.en.curso");
        }
        long unconfirmed = assignment.countDays(day -> day.getStatus() != WorkDayStatus.CONFIRMED);
        if (unconfirmed > 0) {
            throw new BusinessRuleException("pago.final.jornadas.sin.confirmar", unconfirmed);
        }
        BigDecimal amount = assignment.calculateBalance();
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("pago.final.sin.saldo");
        }
        LocalDate periodStart = assignment.nextUnsettledDate();
        LocalDate lastDay = assignment.lastWorkDayDate().orElse(today);
        LocalDate periodEnd = lastDay.isBefore(periodStart) ? periodStart : lastDay;
        BigDecimal earnedInPeriod = assignment.calculateEarnedConfirmed()
                .subtract(assignment.calculateEarnedConfirmedUntil(periodStart.minusDays(1)));
        return new PaymentQuote(amount, periodStart, periodEnd, earnedInPeriod, earnedInPeriod.subtract(amount));
    }

    @Override
    public Payment create(Assignment assignment, PaymentCommand command, String receiptNumber, LocalDateTime now) {
        PaymentQuote quote = quote(assignment, command, now.toLocalDate());
        return new Settlement(assignment, receiptNumber, quote.amount(), command.method(),
                command.transactionReference(), now, quote.periodStart(), quote.periodEnd(), true);
    }
}
