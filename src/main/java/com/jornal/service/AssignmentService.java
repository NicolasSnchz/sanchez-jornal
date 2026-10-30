package com.jornal.service;

import com.jornal.dto.form.CompletionForm;
import com.jornal.dto.form.InvitationForm;
import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.AssignmentRow;
import com.jornal.dto.view.CandidateRow;
import com.jornal.security.Policies;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/** Ciclo de vida de las asignaciones: invitar, responder, cancelar, finalizar y calificar. */
public interface AssignmentService {

    // ------------------------------------------------------------ maestro

    @PreAuthorize(Policies.FOREMAN_SELF)
    List<CandidateRow> findCandidates(UUID foremanId, UUID frontId, String name);

    @PreAuthorize(Policies.FOREMAN_SELF)
    CandidateRow getCandidate(UUID foremanId, UUID frontId, UUID workerId);

    @PreAuthorize(Policies.FOREMAN_SELF)
    UUID invite(UUID foremanId, UUID frontId, InvitationForm form);

    /** Cuadrilla: invitaciones abiertas y asignaciones en curso de todas las obras del maestro. */
    @PreAuthorize(Policies.FOREMAN_SELF)
    List<AssignmentRow> listCrew(UUID foremanId);

    /** @param week cualquier dia de la semana que se quiere ver en la planilla; null para la actual */
    @PreAuthorize(Policies.FOREMAN_SELF)
    AssignmentDetail getForForeman(UUID foremanId, UUID assignmentId, LocalDate week);

    @PreAuthorize(Policies.FOREMAN_SELF)
    void cancel(UUID foremanId, UUID assignmentId, String reason);

    @PreAuthorize(Policies.FOREMAN_SELF)
    void complete(UUID foremanId, UUID assignmentId, CompletionForm form);

    // ------------------------------------------------------------ trabajador

    @PreAuthorize(Policies.WORKER_SELF)
    List<AssignmentRow> listForWorker(UUID workerId);

    @PreAuthorize(Policies.WORKER_SELF)
    AssignmentDetail getForWorker(UUID workerId, UUID assignmentId, LocalDate week);

    @PreAuthorize(Policies.WORKER_SELF)
    void respond(UUID workerId, UUID assignmentId, boolean accept);

    @PreAuthorize(Policies.WORKER_SELF)
    void rateForeman(UUID workerId, UUID assignmentId, int rating);
}
