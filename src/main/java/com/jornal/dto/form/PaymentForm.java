package com.jornal.dto.form;

import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.strategy.PaymentCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago a un trabajador. Segun el tipo se usan unos campos u otros: el anticipo
 * lleva monto y motivo; la liquidacion de periodo lleva fecha de corte; la
 * liquidacion final no lleva monto porque es el saldo completo.
 */
public class PaymentForm {

    @NotNull
    private PaymentType type;

    @Positive
    private BigDecimal amount;

    @NotNull
    private PaymentMethod method = PaymentMethod.CASH;

    @Size(max = 40)
    private String transactionReference;

    @Size(max = 200)
    private String reason;

    private boolean requestedByWorker;

    private LocalDate periodEnd;

    /** Las transferencias y billeteras dejan un numero de operacion; se exige para poder rastrear el pago. */
    @AssertTrue(message = "{validacion.referencia.requerida}")
    public boolean isReferencePresentWhenRequired() {
        return method == null || method == PaymentMethod.CASH
                || (transactionReference != null && !transactionReference.isBlank());
    }

    public PaymentCommand toCommand() {
        return new PaymentCommand(type, amount, method, transactionReference, reason, requestedByWorker, periodEnd);
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isRequestedByWorker() {
        return requestedByWorker;
    }

    public void setRequestedByWorker(boolean requestedByWorker) {
        this.requestedByWorker = requestedByWorker;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
}
