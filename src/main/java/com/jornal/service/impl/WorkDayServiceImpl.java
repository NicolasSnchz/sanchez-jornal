package com.jornal.service.impl;

import com.jornal.dto.form.WorkDayForm;
import com.jornal.dto.view.WorkDayCorrection;
import com.jornal.dto.view.WorkDayRow;
import com.jornal.entity.Assignment;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkDayStatus;
import com.jornal.entity.WorkFront;
import com.jornal.exception.BusinessRuleException;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.WorkDayRepository;
import com.jornal.service.ViewMapper;
import com.jornal.service.WorkDayService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkDayServiceImpl implements WorkDayService {

    private static final Logger log = LoggerFactory.getLogger(WorkDayServiceImpl.class);
    /** Cuantos dias hacia atras se busca un dia sin registrar para sugerirlo. */
    private static final int SUGGESTION_WINDOW_DAYS = 14;

    private final AssignmentRepository assignmentRepository;
    private final WorkDayRepository workDayRepository;
    private final ViewMapper mapper;
    private final Clock clock;

    public WorkDayServiceImpl(AssignmentRepository assignmentRepository, WorkDayRepository workDayRepository,
                              ViewMapper mapper, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.workDayRepository = workDayRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Precarga el horario del frente; el maestro solo cambia lo que fue distinto ese dia. */
    @Override
    @Transactional(readOnly = true)
    public WorkDayForm prepareWorkDay(UUID foremanId, UUID assignmentId, LocalDate date) {
        Assignment assignment = findForForeman(foremanId, assignmentId);
        WorkFront front = assignment.getFront();
        WorkDayForm form = new WorkDayForm();
        form.setDate(date != null ? date : suggestDate(assignment));
        form.setCheckInTime(front.getStartTime());
        form.setCheckOutTime(front.getEndTime());
        form.setBreakMinutes(front.getLunchBreakMinutes());
        form.setTaskPerformed(front.getTask());
        return form;
    }

    @Override
    public UUID registerWorkDay(UUID foremanId, UUID assignmentId, WorkDayForm form) {
        Assignment assignment = findForForeman(foremanId, assignmentId);
        WorkDay day = assignment.registerWorkDay(form.getDate(), form.toRecord(), LocalDate.now(clock),
                LocalDateTime.now(clock));
        workDayRepository.save(day);
        log.info("Jornada {} registrada en la asignacion {}", day.getId(), assignmentId);
        return day.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkDayCorrection getForCorrection(UUID foremanId, UUID workDayId) {
        WorkDay day = findWorkDayForForeman(foremanId, workDayId);
        WorkDayRow row = mapper.toRow(day);
        if (!row.editable()) {
            throw new BusinessRuleException("jornada.no.editable");
        }
        return new WorkDayCorrection(day.getId(), day.getAssignment().getId(), row, WorkDayForm.from(day));
    }

    /** La fecha no se corrige: si el dia fue otro, se registra ese dia y este se deja objetado. */
    @Override
    public UUID correctWorkDay(UUID foremanId, UUID workDayId, WorkDayForm form) {
        WorkDay day = findWorkDayForForeman(foremanId, workDayId);
        if (form.getDate() != null && !form.getDate().equals(day.getDate())) {
            throw new BusinessRuleException("jornada.corregir.fecha");
        }
        Assignment assignment = day.getAssignment();
        assignment.correctWorkDay(day, form.toRecord(), LocalDateTime.now(clock));
        log.info("Jornada {} corregida", workDayId);
        return assignment.getId();
    }

    @Override
    public UUID confirm(UUID workerId, UUID workDayId) {
        WorkDay day = findForWorker(workerId, workDayId);
        day.confirm(LocalDateTime.now(clock));
        log.info("Jornada {} confirmada por el trabajador", workDayId);
        return day.getAssignment().getId();
    }

    @Override
    public int confirmAllPending(UUID workerId, UUID assignmentId) {
        Assignment assignment = assignmentRepository.findByIdAndWorkerId(assignmentId, workerId)
                .orElseThrow(() -> new ResourceNotFoundException("asignacion.no.encontrada"));
        LocalDateTime now = LocalDateTime.now(clock);
        List<WorkDay> pending = assignment.getWorkDays().stream()
                .filter(day -> day.getStatus() == WorkDayStatus.PENDING)
                .toList();
        pending.forEach(day -> day.confirm(now));
        log.info("Asignacion {}: {} jornadas confirmadas en bloque", assignmentId, pending.size());
        return pending.size();
    }

    @Override
    public UUID dispute(UUID workerId, UUID workDayId, String reason) {
        WorkDay day = findForWorker(workerId, workDayId);
        day.dispute(reason, LocalDateTime.now(clock));
        log.info("Jornada {} objetada por el trabajador", workDayId);
        return day.getAssignment().getId();
    }

    /**
     * Sugiere el dia laboral mas reciente sin registro (hoy primero), dentro del
     * periodo de la asignacion y despues de lo ya liquidado. Si no hay, hoy.
     */
    private LocalDate suggestDate(Assignment assignment) {
        LocalDate today = LocalDate.now(clock);
        WorkFront front = assignment.getFront();
        LocalDate first = assignment.nextUnsettledDate();
        LocalDate last = front.getEndDate().isBefore(today) ? front.getEndDate() : today;
        for (int i = 0; i < SUGGESTION_WINDOW_DAYS; i++) {
            LocalDate candidate = last.minusDays(i);
            if (candidate.isBefore(first)) {
                break;
            }
            if (front.isWorkingDay(candidate) && assignment.findWorkDayOn(candidate).isEmpty()) {
                return candidate;
            }
        }
        return last;
    }

    private Assignment findForForeman(UUID foremanId, UUID assignmentId) {
        return assignmentRepository.findByIdAndFrontSiteForemanId(assignmentId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("asignacion.no.encontrada"));
    }

    private WorkDay findWorkDayForForeman(UUID foremanId, UUID workDayId) {
        return workDayRepository.findByIdAndAssignmentFrontSiteForemanId(workDayId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("jornada.no.encontrada"));
    }

    private WorkDay findForWorker(UUID workerId, UUID workDayId) {
        return workDayRepository.findByIdAndAssignmentWorkerId(workDayId, workerId)
                .orElseThrow(() -> new ResourceNotFoundException("jornada.no.encontrada"));
    }
}
