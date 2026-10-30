package com.jornal.service;

import com.jornal.dto.form.WorkDayForm;
import com.jornal.dto.view.WorkDayCorrection;
import com.jornal.security.Policies;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Registro de jornadas. El maestro registra y corrige; el trabajador confirma u
 * objeta. Ninguno puede hacer la parte del otro: asi el saldo solo cuenta horas
 * que ambos aceptaron.
 */
public interface WorkDayService {

    /** Formulario precargado con el horario del frente para la fecha indicada (o la sugerida). */
    @PreAuthorize(Policies.FOREMAN_SELF)
    WorkDayForm prepareWorkDay(UUID foremanId, UUID assignmentId, LocalDate date);

    @PreAuthorize(Policies.FOREMAN_SELF)
    UUID registerWorkDay(UUID foremanId, UUID assignmentId, WorkDayForm form);

    @PreAuthorize(Policies.FOREMAN_SELF)
    WorkDayCorrection getForCorrection(UUID foremanId, UUID workDayId);

    /** @return id de la asignacion, para volver a su planilla */
    @PreAuthorize(Policies.FOREMAN_SELF)
    UUID correctWorkDay(UUID foremanId, UUID workDayId, WorkDayForm form);

    @PreAuthorize(Policies.WORKER_SELF)
    UUID confirm(UUID workerId, UUID workDayId);

    /** Confirma de una vez todas las jornadas por confirmar (no toca las objetadas). */
    @PreAuthorize(Policies.WORKER_SELF)
    int confirmAllPending(UUID workerId, UUID assignmentId);

    @PreAuthorize(Policies.WORKER_SELF)
    UUID dispute(UUID workerId, UUID workDayId, String reason);
}
