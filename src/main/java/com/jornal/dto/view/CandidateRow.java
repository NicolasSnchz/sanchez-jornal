package com.jornal.dto.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jornal.entity.Trade;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Trabajador que aparece al invitar, con los motivos por los que no seria apto. */
public record CandidateRow(
        UUID workerId,
        String fullName,
        String initials,
        Trade trade,
        int yearsOfExperience,
        String neighborhood,
        boolean hasOwnTools,
        LocalDate arlValidUntil,
        Double rating,
        long ratingCount,
        long confirmedWorkDays,
        List<String> issues) {

    /** Se publica tambien en el JSON de la API: el cliente no tiene que deducirlo de la lista de motivos. */
    @JsonProperty("eligible")
    public boolean eligible() {
        return issues.isEmpty();
    }
}
