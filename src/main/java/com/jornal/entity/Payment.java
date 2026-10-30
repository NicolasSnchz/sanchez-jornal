package com.jornal.entity;

import com.jornal.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Dinero entregado al trabajador dentro de una asignacion. Es abstracta: todo
 * pago es un anticipo o una liquidacion. Un pago nunca se borra: se anula
 * dejando fecha y motivo, para que el historial contable quede completo.
 */
@Entity
@Table(name = "payments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 20)
    private String receiptNumber;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "transaction_reference", length = 40)
    private String transactionReference;

    /** El trabajador confirma que recibio el dinero. */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected Payment() {
        // requerido por JPA
    }

    protected Payment(Assignment assignment, String receiptNumber, BigDecimal amount, PaymentMethod method,
                      String transactionReference, LocalDateTime paidAt) {
        this.assignment = Objects.requireNonNull(assignment, "assignment");
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("pago.monto.invalido");
        }
        this.receiptNumber = Objects.requireNonNull(receiptNumber, "receiptNumber");
        this.amount = amount.setScale(0, RoundingMode.HALF_UP);
        this.method = Objects.requireNonNull(method, "method");
        this.transactionReference = transactionReference == null || transactionReference.isBlank()
                ? null : transactionReference.trim();
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt");
        assignment.addPayment(this);
    }

    public abstract PaymentType getType();

    public PaymentStatus getStatus() {
        if (cancelledAt != null) {
            return PaymentStatus.CANCELLED;
        }
        return confirmedAt != null ? PaymentStatus.CONFIRMED : PaymentStatus.PENDING;
    }

    /** Un pago vigente (no anulado) cuenta en el saldo. */
    public boolean isEffective() {
        return cancelledAt == null;
    }

    public void confirmReceipt(LocalDateTime now) {
        if (getStatus() != PaymentStatus.PENDING) {
            throw new BusinessRuleException("pago.confirmar.estado.invalido");
        }
        confirmedAt = now;
    }

    /**
     * Solo se anula un pago que el trabajador aun no ha confirmado haber recibido,
     * de una asignacion abierta y que no rompa los periodos ya liquidados.
     */
    public void cancel(String reason, LocalDateTime now) {
        cancellationIssue().ifPresent(code -> {
            throw new BusinessRuleException(code);
        });
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("pago.anular.motivo.requerido");
        }
        cancellationReason = reason.trim();
        cancelledAt = now;
    }

    /** La vista usa la misma regla para mostrar u ocultar el boton de anular. */
    public boolean canBeCancelled() {
        return cancellationIssue().isEmpty();
    }

    /** Motivo por el que no se puede anular; las subclases agregan sus propias reglas. */
    protected Optional<String> cancellationIssue() {
        if (getStatus() != PaymentStatus.PENDING) {
            return Optional.of("pago.anular.estado.invalido");
        }
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            return Optional.of("pago.anular.asignacion.finalizada");
        }
        return Optional.empty();
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }
}
