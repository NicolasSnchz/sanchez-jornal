package com.jornal.dto.view;

import com.jornal.entity.SiteStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detalle de una obra con sus frentes, sus trabajadores y las acciones permitidas. */
public record SiteDetail(
        UUID id,
        String name,
        String description,
        String address,
        String neighborhood,
        String city,
        String buildingPermit,
        LocalDate startDate,
        LocalDate estimatedEndDate,
        SiteStatus status,
        boolean delayed,
        BigDecimal laborCost,
        long activeAssignments,
        long workersInProgress,
        List<FrontSummary> fronts,
        List<AssignmentRow> assignments,
        boolean canEdit,
        boolean canStart,
        boolean canSuspend,
        boolean canResume,
        boolean canFinish,
        boolean canOpenFront) {
}
