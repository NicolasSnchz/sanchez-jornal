package com.jornal.dto.view;

import com.jornal.entity.SiteStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Tarjeta de obra para listados y panel. */
public record SiteSummary(
        UUID id,
        String name,
        String address,
        String neighborhood,
        String city,
        SiteStatus status,
        boolean delayed,
        LocalDate startDate,
        LocalDate estimatedEndDate,
        int frontsCount,
        long workersInProgress,
        BigDecimal laborCost) {
}
