package com.jornal.dto.view;

import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentStatus;
import com.jornal.entity.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Pago listo para mostrar en tablas y comprobantes. */
public record PaymentRow(
        UUID id,
        UUID assignmentId,
        String receiptNumber,
        PaymentType type,
        LocalDateTime paidAt,
        BigDecimal amount,
        PaymentMethod method,
        String transactionReference,
        PaymentStatus status,
        String reason,
        Boolean requestedByWorker,
        LocalDate periodStart,
        LocalDate periodEnd,
        String cancellationReason,
        String workerName,
        String siteName,
        String frontTask,
        boolean canCancel,
        boolean canConfirm) {
}
