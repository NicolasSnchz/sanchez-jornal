package com.jornal.dto.view;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Panel del maestro. */
public record ForemanDashboard(
        String firstName,
        long activeSites,
        long delayedSites,
        long workersInProgress,
        long sitesWithWorkers,
        YearMonth month,
        BigDecimal monthSpending,
        long monthPaymentsCount,
        Double rating,
        long ratingCount,
        List<PendingItem> pending,
        List<SiteSummary> sitesInProgress,
        List<PaymentRow> recentPayments,
        List<WeekCost> weeklyCost) {
}
