package com.jornal.strategy;

import com.jornal.entity.Advance;
import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentType;
import com.jornal.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Anticipo: adelanto que no puede superar el saldo, es decir, lo trabajado y
 * confirmado que aun no se ha pagado. Asi nunca se paga trabajo que no existe.
 */
@Component
public class AdvancePaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentType type() {
        return PaymentType.ADVANCE;
    }

    @Override
    public PaymentQuote quote(Assignment assignment, PaymentCommand command, LocalDate today) {
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new BusinessRuleException("pago.asignacion.no.en.curso");
        }
        BigDecimal amount = command.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("pago.monto.invalido");
        }
        BigDecimal balance = assignment.calculateBalance();
        if (amount.compareTo(balance) > 0) {
            throw new BusinessRuleException("pago.anticipo.excede.saldo", balance);
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new BusinessRuleException("pago.anticipo.motivo.requerido");
        }
        return PaymentQuote.ofAmount(amount);
    }

    @Override
    public Payment create(Assignment assignment, PaymentCommand command, String receiptNumber, LocalDateTime now) {
        PaymentQuote quote = quote(assignment, command, now.toLocalDate());
        return new Advance(assignment, receiptNumber, quote.amount(), command.method(),
                command.transactionReference(), now, command.reason(), command.requestedByWorker());
    }
}
