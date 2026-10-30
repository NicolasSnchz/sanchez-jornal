package com.jornal.dto.view;

import com.jornal.entity.SiteStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Detalle de un frente con sus asignaciones. */
public record FrontDetail(
        UUID siteId,
        String siteName,
        String siteCity,
        SiteStatus siteStatus,
        FrontSummary front,
        List<AssignmentRow> assignments,
        BigDecimal minimumDailyWage) {
}
