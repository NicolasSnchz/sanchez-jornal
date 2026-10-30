package com.jornal.service.impl;

import com.jornal.dto.form.CompletionForm;
import com.jornal.dto.form.InvitationForm;
import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.AssignmentRow;
import com.jornal.dto.view.CandidateRow;
import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.SiteStatus;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.exception.BusinessRuleException;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.WorkFrontRepository;
import com.jornal.repository.WorkerRepository;
import com.jornal.service.AssignmentService;
import com.jornal.service.ViewMapper;
import com.jornal.service.WagePolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentServiceImpl.class);
    private static final int MAX_CANDIDATES = 25;
    private static final Set<AssignmentStatus> ACTIVE = EnumSet.of(AssignmentStatus.INVITED, AssignmentStatus.IN_PROGRESS);

    private final AssignmentRepository assignmentRepository;
    private final WorkFrontRepository frontRepository;
    private final WorkerRepository workerRepository;
    private final ViewMapper mapper;
    private final WagePolicy wagePolicy;
    private final Clock clock;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository, WorkFrontRepository frontRepository,
                                 WorkerRepository workerRepository, ViewMapper mapper, WagePolicy wagePolicy,
                                 Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.frontRepository = frontRepository;
        this.workerRepository = workerRepository;
        this.mapper = mapper;
        this.wagePolicy = wagePolicy;
        this.clock = clock;
    }

    // ================================================================== maestro

    /**
     * Candidatos del mismo oficio y ciudad de la obra que reciben ofertas. Se
     * muestran tambien los que no son aptos, con el motivo, para que el maestro
     * entienda por que no puede invitarlos (por ejemplo, la ARL vence antes).
     */
    @Override
    @Transactional(readOnly = true)
    public List<CandidateRow> findCandidates(UUID foremanId, UUID frontId, String name) {
        WorkFront front = findFront(foremanId, frontId);
        String filter = name == null ? "" : name.trim();
        LocalDate from = earliestStart(front);
        return workerRepository.searchCandidates(front.getRequiredTrade(), front.getSite().getCity(), filter,
                        PageRequest.of(0, MAX_CANDIDATES)).stream()
                .map(worker -> toCandidate(worker, front, from))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateRow getCandidate(UUID foremanId, UUID frontId, UUID workerId) {
        WorkFront front = findFront(foremanId, frontId);
        return toCandidate(findWorker(workerId), front, earliestStart(front));
    }

    @Override
    public UUID invite(UUID foremanId, UUID frontId, InvitationForm form) {
        WorkFront front = findFront(foremanId, frontId);
        Worker worker = findWorker(form.getWorkerId());
        List<String> issues = eligibilityIssues(worker, front, form.getStartDate());
        if (!issues.isEmpty()) {
            throw new BusinessRuleException(issues.get(0));
        }
        wagePolicy.requireLegalWage(form.getDailyWage(), front.scheduledMinutesPerDay(),
                "invitacion.jornal.bajo.minimo");
        Assignment assignment = Assignment.invite(front, worker, form.getDailyWage(), form.getStartDate(),
                LocalDateTime.now(clock));
        assignmentRepository.save(assignment);
        log.info("Invitacion {} enviada al trabajador {} para el frente {}", assignment.getId(), worker.getId(),
                frontId);
        return assignment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRow> listCrew(UUID foremanId) {
        return assignmentRepository.findByFrontSiteForemanIdAndStatusInOrderByInvitedAtDesc(foremanId, ACTIVE)
                .stream()
                .map(mapper::toRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDetail getForForeman(UUID foremanId, UUID assignmentId, LocalDate week) {
        return mapper.toDetail(findForForeman(foremanId, assignmentId), week);
    }

    @Override
    public void cancel(UUID foremanId, UUID assignmentId, String reason) {
        findForForeman(foremanId, assignmentId).cancel(reason, LocalDateTime.now(clock));
        log.info("Asignacion {} cancelada por el maestro", assignmentId);
    }

    @Override
    public void complete(UUID foremanId, UUID assignmentId, CompletionForm form) {
        findForForeman(foremanId, assignmentId).complete(form.getRating(), form.getComment(), LocalDate.now(clock));
        log.info("Asignacion {} finalizada", assignmentId);
    }

    // ================================================================== trabajador

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRow> listForWorker(UUID workerId) {
        return assignmentRepository.findByWorkerIdOrderByInvitedAtDesc(workerId).stream()
                .map(mapper::toRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDetail getForWorker(UUID workerId, UUID assignmentId, LocalDate week) {
        return mapper.toDetail(findForWorker(workerId, assignmentId), week);
    }

    /**
     * Al aceptar se repiten las validaciones que pudieron cambiar desde la
     * invitacion: el trabajador pudo aceptar otra obra en las mismas fechas, la
     * obra pudo suspenderse o el frente pudo terminar.
     */
    @Override
    public void respond(UUID workerId, UUID assignmentId, boolean accept) {
        Assignment assignment = findForWorker(workerId, assignmentId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!accept) {
            assignment.reject(now);
            log.info("Invitacion {} rechazada", assignmentId);
            return;
        }
        WorkFront front = assignment.getFront();
        SiteStatus siteStatus = front.getSite().getStatus();
        if (siteStatus != SiteStatus.PLANNED && siteStatus != SiteStatus.IN_PROGRESS) {
            throw new BusinessRuleException("invitacion.obra.no.admite");
        }
        if (front.getEndDate().isBefore(now.toLocalDate())) {
            throw new BusinessRuleException("asignacion.aceptar.frente.vencido");
        }
        if (!assignment.getWorker().hasArlValidThrough(front.getEndDate())) {
            throw new BusinessRuleException("elegibilidad.arl.vence.antes");
        }
        if (assignmentRepository.existsInProgressOverlapping(workerId, assignment.getStartDate(), front.getEndDate())) {
            throw new BusinessRuleException("asignacion.aceptar.agenda.cruzada");
        }
        assignment.accept(now);
        log.info("Invitacion {} aceptada", assignmentId);
    }

    @Override
    public void rateForeman(UUID workerId, UUID assignmentId, int rating) {
        findForWorker(workerId, assignmentId).rateForeman(rating);
        log.info("Asignacion {}: el trabajador califico al maestro", assignmentId);
    }

    // ================================================================== apoyo

    /**
     * Reglas que dependen del trabajador (en la entidad) mas las que necesitan
     * la base de datos o la obra: ciudad, invitacion repetida y cruce de agenda.
     */
    private List<String> eligibilityIssues(Worker worker, WorkFront front, LocalDate startDate) {
        List<String> issues = new ArrayList<>(worker.eligibilityIssues(front, LocalDate.now(clock)));
        if (!worker.getCity().equalsIgnoreCase(front.getSite().getCity())) {
            issues.add("elegibilidad.otra.ciudad");
        }
        if (assignmentRepository.existsByWorkerIdAndFrontIdAndStatusIn(worker.getId(), front.getId(), ACTIVE)) {
            // Si ya esta en este frente, el cruce de agenda seria consigo mismo: no se repite el motivo.
            issues.add("elegibilidad.ya.invitado");
        } else if (assignmentRepository.existsInProgressOverlapping(worker.getId(), startDate, front.getEndDate())) {
            issues.add("elegibilidad.agenda.cruzada");
        }
        return issues;
    }

    private CandidateRow toCandidate(Worker worker, WorkFront front, LocalDate from) {
        UUID id = worker.getId();
        return new CandidateRow(id, worker.getFullName(), worker.getInitials(), worker.getTrade(),
                worker.getYearsOfExperience(), worker.getNeighborhood(), worker.isHasOwnTools(),
                worker.getArlValidUntil(), workerRepository.averageRating(id), workerRepository.countRatings(id),
                workerRepository.countConfirmedWorkDays(id), eligibilityIssues(worker, front, from));
    }

    /** Primer dia en que podria empezar un trabajador invitado hoy. */
    private LocalDate earliestStart(WorkFront front) {
        LocalDate today = LocalDate.now(clock);
        return front.getStartDate().isAfter(today) ? front.getStartDate() : today;
    }

    private WorkFront findFront(UUID foremanId, UUID frontId) {
        return frontRepository.findByIdAndSiteForemanId(frontId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("frente.no.encontrado"));
    }

    /** Un trabajador desactivado no aparece para nadie. */
    private Worker findWorker(UUID workerId) {
        return workerRepository.findById(workerId)
                .filter(Worker::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("trabajador.no.encontrado"));
    }

    private Assignment findForForeman(UUID foremanId, UUID assignmentId) {
        return assignmentRepository.findByIdAndFrontSiteForemanId(assignmentId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("asignacion.no.encontrada"));
    }

    private Assignment findForWorker(UUID workerId, UUID assignmentId) {
        return assignmentRepository.findByIdAndWorkerId(assignmentId, workerId)
                .orElseThrow(() -> new ResourceNotFoundException("asignacion.no.encontrada"));
    }
}
