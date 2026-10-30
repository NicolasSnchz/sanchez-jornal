package com.jornal.dto.view;

import com.jornal.entity.DocumentType;
import com.jornal.entity.Role;
import java.time.LocalDateTime;

/** Encabezado de solo lectura del perfil: lo que identifica la cuenta y no se edita. */
public record ProfileSummary(
        Role role,
        String fullName,
        String initials,
        String email,
        DocumentType documentType,
        String documentNumber,
        int age,
        LocalDateTime memberSince,
        Double rating,
        long ratingCount,
        long confirmedWorkDays,
        long completedAssignments) {
}
