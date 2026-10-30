package com.jornal.dto.view;

import com.jornal.entity.DocumentType;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentStatus;
import com.jornal.entity.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Comprobante de pago imprimible. Para las liquidaciones incluye el desglose:
 * lo devengado en el periodo menos los anticipos ya entregados da el neto pagado.
 */
public record Receipt(
        UUID paymentId,
        UUID assignmentId,
        String receiptNumber,
        PaymentType type,
        PaymentStatus status,
        LocalDateTime paidAt,
        BigDecimal amount,
        String amountInWords,
        PaymentMethod method,
        String transactionReference,
        String reason,
        LocalDate periodStart,
        LocalDate periodEnd,
        long daysInPeriod,
        BigDecimal earnedInPeriod,
        BigDecimal discountedAdvances,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        String cancellationReason,
        String foremanName,
        String foremanCompany,
        DocumentType foremanDocumentType,
        String foremanDocumentNumber,
        String workerName,
        DocumentType workerDocumentType,
        String workerDocumentNumber,
        String siteName,
        String siteAddress,
        String siteCity,
        String frontTask,
        BigDecimal dailyWage) {

    public boolean isSettlement() {
        return type != PaymentType.ADVANCE;
    }
}
