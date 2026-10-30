package com.jornal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/** Anticipo: adelanto sobre trabajo ya confirmado, antes de liquidar el periodo. */
@Entity
@DiscriminatorValue("ADVANCE")
public class Advance extends Payment {

    @Column(length = 200)
    private String reason;

    /** true si lo pidio el trabajador; false si lo ofrecio el maestro. */
    @Column(name = "requested_by_worker")
    private Boolean requestedByWorker;

    protected Advance() {
        // requerido por JPA
    }

    public Advance(Assignment assignment, String receiptNumber, BigDecimal amount, PaymentMethod method,
                   String transactionReference, LocalDateTime paidAt, String reason, boolean requestedByWorker) {
        super(assignment, receiptNumber, amount, method, transactionReference, paidAt);
        this.reason = reason.trim();
        this.requestedByWorker = requestedByWorker;
    }

    @Override
    public PaymentType getType() {
        return PaymentType.ADVANCE;
    }

    /**
     * Un anticipo que ya fue descontado en una liquidacion posterior no se puede
     * anular: el neto de esa liquidacion quedaria mal calculado.
     */
    @Override
    protected Optional<String> cancellationIssue() {
        Optional<String> issue = super.cancellationIssue();
        if (issue.isPresent()) {
            return issue;
        }
        boolean discounted = getAssignment().latestEffectiveSettlement()
                .map(settlement -> settlement.getPaidAt().isAfter(getPaidAt()))
                .orElse(false);
        return discounted ? Optional.of("pago.anular.anticipo.descontado") : Optional.empty();
    }

    public String getReason() {
        return reason;
    }

    public boolean isRequestedByWorker() {
        return Boolean.TRUE.equals(requestedByWorker);
    }
}
