package com.jornal.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Panel del trabajador. */
public record WorkerDashboard(
        String firstName,
        BigDecimal balance,
        BigDecimal unconfirmed,
        long confirmedWorkDays,
        Double rating,
        long ratingCount,
        long completedAssignments,
        LocalDate arlValidUntil,
        boolean arlExpiringSoon,
        boolean acceptingOffers,
        List<PendingItem> pending,
        List<AssignmentDetail> current,
        List<AssignmentRow> invitations) {
}
