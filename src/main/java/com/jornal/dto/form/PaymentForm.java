package com.jornal.dto.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.strategy.PaymentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Pago: anticipo, liquidaci\u00f3n de periodo o liquidaci\u00f3n final")
public class PaymentForm {

    @Schema(description = "Tipo de pago", example = "ADVANCE")
    @NotNull
    private PaymentType type;

    @Schema(description = "Valor en pesos; solo para anticipos (las liquidaciones se calculan solas)",
            example = "50000")
    @Positive
    private BigDecimal amount;

    @Schema(description = "Medio de pago", example = "CASH")
    @NotNull
    private PaymentMethod method = PaymentMethod.CASH;

    @Schema(description = "Referencia de la transacci\u00f3n; obligatoria si no es efectivo", example = "NEQUI-883421")
    @Size(max = 40)
    private String transactionReference;

    @Schema(description = "Motivo del anticipo (opcional)", example = "Pasajes de la semana")
    @Size(max = 200)
    private String reason;

    @Schema(description = "Si el trabajador pidi\u00f3 el anticipo", example = "true")
    private boolean requestedByWorker;

    @Schema(description = "Fecha de corte de una liquidaci\u00f3n de periodo", example = "2026-10-23")
    private LocalDate periodEnd;

    /** Las transferencias y billeteras dejan un numero de operacion; se exige para poder rastrear el pago. */
    @AssertTrue(message = "{validacion.referencia.requerida}")
    @Schema(hidden = true)
    @JsonIgnore
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
