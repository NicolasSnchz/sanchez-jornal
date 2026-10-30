package com.jornal.service;

import com.jornal.dto.form.PaymentForm;
import com.jornal.dto.view.PaymentRow;
import com.jornal.dto.view.PaymentsMonth;
import com.jornal.dto.view.Receipt;
import com.jornal.security.Policies;
import com.jornal.strategy.PaymentQuote;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/** Anticipos y liquidaciones. Cada tipo de pago aplica sus reglas con su propia estrategia. */
public interface PaymentService {

    /** Calcula el pago sin registrarlo, para que el maestro vea el monto antes de confirmar. */
    @PreAuthorize(Policies.FOREMAN_SELF)
    PaymentQuote quote(UUID foremanId, UUID assignmentId, PaymentForm form);

    @PreAuthorize(Policies.FOREMAN_SELF)
    PaymentRow register(UUID foremanId, UUID assignmentId, PaymentForm form);

    /** @return id de la asignacion del pago anulado */
    @PreAuthorize(Policies.FOREMAN_SELF)
    UUID cancel(UUID foremanId, UUID paymentId, String reason);

    @PreAuthorize(Policies.FOREMAN_SELF)
    PaymentsMonth listMonth(UUID foremanId, YearMonth month);

    @PreAuthorize(Policies.FOREMAN_SELF)
    Receipt receiptForForeman(UUID foremanId, UUID paymentId);

    /** @return id de la asignacion del pago confirmado */
    @PreAuthorize(Policies.WORKER_SELF)
    UUID confirmReceipt(UUID workerId, UUID paymentId);

    @PreAuthorize(Policies.WORKER_SELF)
    Receipt receiptForWorker(UUID workerId, UUID paymentId);
}
