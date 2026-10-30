package com.jornal.dto.view;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Pagos de un mes con sus totales (los pagos anulados no suman). */
public record PaymentsMonth(
        YearMonth month,
        YearMonth previousMonth,
        YearMonth nextMonth,
        boolean hasNext,
        BigDecimal total,
        BigDecimal advancesTotal,
        long advancesCount,
        BigDecimal settlementsTotal,
        long settlementsCount,
        long pendingConfirmation,
        long cancelledCount,
        List<PaymentRow> payments) {
}
