package com.jornal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Liquidacion: paga lo devengado y confirmado hasta el fin del periodo, ya
 * descontados los anticipos. Los periodos van seguidos y sin huecos; cerrar un
 * periodo bloquea la edicion de sus jornadas.
 */
@Entity
@DiscriminatorValue("SETTLEMENT")
public class Settlement extends Payment {

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    /** La liquidacion final deja el saldo en cero y cierra el registro de jornadas. */
    @Column(name = "final_settlement")
    private Boolean finalSettlement;

    protected Settlement() {
        // requerido por JPA
    }

    public Settlement(Assignment assignment, String receiptNumber, BigDecimal amount, PaymentMethod method,
                      String transactionReference, LocalDateTime paidAt, LocalDate periodStart,
                      LocalDate periodEnd, boolean finalSettlement) {
        super(assignment, receiptNumber, amount, method, transactionReference, paidAt);
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.finalSettlement = finalSettlement;
    }

    @Override
    public PaymentType getType() {
        return isFinalSettlement() ? PaymentType.FINAL_SETTLEMENT : PaymentType.SETTLEMENT;
    }

    /** Solo se anula la ultima liquidacion vigente; asi los periodos nunca quedan con huecos. */
    @Override
    protected Optional<String> cancellationIssue() {
        Optional<String> issue = super.cancellationIssue();
        if (issue.isPresent()) {
            return issue;
        }
        boolean isLatest = getAssignment().latestEffectiveSettlement().map(this::equals).orElse(false);
        return isLatest ? Optional.empty() : Optional.of("pago.anular.liquidacion.no.ultima");
    }

    public boolean includes(LocalDate date) {
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd);
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public boolean isFinalSettlement() {
        return Boolean.TRUE.equals(finalSettlement);
    }
}
