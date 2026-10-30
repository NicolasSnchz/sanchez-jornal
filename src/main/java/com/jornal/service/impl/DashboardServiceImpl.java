package com.jornal.service.impl;

import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.AssignmentRow;
import com.jornal.dto.view.ForemanDashboard;
import com.jornal.dto.view.PendingItem;
import com.jornal.dto.view.WeekCost;
import com.jornal.dto.view.WorkerDashboard;
import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentStatus;
import com.jornal.entity.SiteStatus;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkDayStatus;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.ForemanRepository;
import com.jornal.repository.PaymentRepository;
import com.jornal.repository.WorkerRepository;
import com.jornal.service.DashboardService;
import com.jornal.service.ViewMapper;
import com.jornal.util.Formats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma los paneles de inicio. Su valor esta en la lista de pendientes: en lugar
 * de que el usuario busque que le falta, el sistema se lo dice y lo lleva directo.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    static final String DANGER = "danger";
    static final String WARNING = "warning";
    static final String INFO = "info";
    static final String NEUTRAL = "neutral";
    private static final List<String> PRIORITY = List.of(DANGER, WARNING, INFO, NEUTRAL);

    private static final int MAX_PENDING = 8;
    private static final int CHART_WEEKS = 6;
    private static final int ARL_WARNING_DAYS = 30;
    private static final Duration INVITATION_REMINDER = Duration.ofHours(48);

    private final ForemanRepository foremanRepository;
    private final WorkerRepository workerRepository;
    private final ConstructionSiteRepository siteRepository;
    private final AssignmentRepository assignmentRepository;
    private final PaymentRepository paymentRepository;
    private final ViewMapper mapper;
    private final Clock clock;

    public DashboardServiceImpl(ForemanRepository foremanRepository, WorkerRepository workerRepository,
                                ConstructionSiteRepository siteRepository, AssignmentRepository assignmentRepository,
                                PaymentRepository paymentRepository, ViewMapper mapper, Clock clock) {
        this.foremanRepository = foremanRepository;
        this.workerRepository = workerRepository;
        this.siteRepository = siteRepository;
        this.assignmentRepository = assignmentRepository;
        this.paymentRepository = paymentRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ================================================================== maestro

    @Override
    public ForemanDashboard foremanDashboard(UUID foremanId) {
        Foreman foreman = foremanRepository.findById(foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("maestro.no.encontrado"));
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);

        List<ConstructionSite> sites = siteRepository.findByForemanIdOrderByCreatedAtDesc(foremanId);
        List<ConstructionSite> inProgress = sites.stream()
                .filter(site -> site.getStatus() == SiteStatus.IN_PROGRESS)
                .toList();
        List<Assignment> assignments = assignmentRepository.findByFrontSiteForemanIdAndStatusInOrderByInvitedAtDesc(
                foremanId, EnumSet.of(AssignmentStatus.INVITED, AssignmentStatus.IN_PROGRESS, AssignmentStatus.COMPLETED));
        List<Assignment> working = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .toList();
        List<Payment> monthPayments = paymentRepository.findByForemanBetween(foremanId,
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        List<Payment> effective = monthPayments.stream().filter(Payment::isEffective).toList();

        return new ForemanDashboard(
                foreman.getFirstName(),
                inProgress.size(),
                inProgress.stream().filter(site -> site.isDelayed(today)).count(),
                working.stream().map(a -> a.getWorker().getId()).distinct().count(),
                working.stream().map(a -> a.getFront().getSite().getId()).distinct().count(),
                month,
                effective.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                effective.size(),
                foremanRepository.averageRating(foremanId),
                foremanRepository.countRatings(foremanId),
                foremanPending(sites, assignments, today),
                inProgress.stream().map(mapper::toSummary).toList(),
                monthPayments.stream().limit(5).map(mapper::toRow).toList(),
                weeklyCost(assignments, today));
    }

    private List<PendingItem> foremanPending(List<ConstructionSite> sites, List<Assignment> assignments,
                                             LocalDate today) {
        List<PendingItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        for (Assignment a : assignments) {
            String worker = a.getWorker().getFullName();
            String link = "/maestro/asignaciones/" + a.getId();
            if (a.getStatus() == AssignmentStatus.INVITED) {
                if (Duration.between(a.getInvitedAt(), now).compareTo(INVITATION_REMINDER) > 0) {
                    items.add(new PendingItem(NEUTRAL, "pendiente.invitacion.sin.respuesta",
                            new Object[] {worker, a.getFront().getTask()}, null, link, "accion.ver", a.getInvitedAt()));
                }
                continue;
            }
            if (a.getStatus() != AssignmentStatus.IN_PROGRESS) {
                continue;
            }
            for (WorkDay day : a.getWorkDays()) {
                if (day.getStatus() == WorkDayStatus.DISPUTED) {
                    items.add(new PendingItem(DANGER, "pendiente.jornada.objetada",
                            new Object[] {worker, Formats.date(day.getDate())}, day.getDisputeReason(),
                            "/maestro/jornadas/" + day.getId() + "/editar", "accion.corregir", day.getDisputedAt()));
                }
            }
            long missing = missingThisWeek(a, today);
            if (missing > 0) {
                items.add(new PendingItem(WARNING, "pendiente.jornadas.sin.registrar",
                        new Object[] {worker, missing}, null, link + "/jornadas/nueva", "accion.registrar", null));
            }
            BigDecimal balance = a.calculateBalance();
            long unconfirmed = a.countDays(day -> day.getStatus() != WorkDayStatus.CONFIRMED);
            if (balance.signum() > 0) {
                items.add(new PendingItem(INFO, "pendiente.saldo.por.pagar",
                        new Object[] {worker, Formats.money(balance)}, null, link + "#pagos", "accion.pagar", null));
            } else if (unconfirmed == 0 && !a.getWorkDays().isEmpty() && a.getFront().getEndDate().isBefore(today)) {
                items.add(new PendingItem(INFO, "pendiente.asignacion.por.finalizar", new Object[] {worker}, null,
                        link + "/finalizar", "accion.finalizar", null));
            }
        }
        for (ConstructionSite site : sites) {
            if (site.isDelayed(today)) {
                items.add(new PendingItem(WARNING, "pendiente.obra.retrasada",
                        new Object[] {site.getName(), Formats.date(site.getEstimatedEndDate())}, null,
                        "/maestro/obras/" + site.getId(), "accion.ver", null));
            }
        }
        return prioritize(items);
    }

    /** Dias laborales de esta semana (hasta ayer) en los que no se registro jornada. */
    private long missingThisWeek(Assignment a, LocalDate today) {
        if (a.hasEffectiveFinalSettlement() || a.getFront().getSite().getStatus() != SiteStatus.IN_PROGRESS) {
            return 0;
        }
        WorkFront front = a.getFront();
        LocalDate from = max(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), a.getStartDate());
        from = max(from, a.nextUnsettledDate());
        LocalDate to = min(today.minusDays(1), front.getEndDate());
        long missing = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (front.isWorkingDay(date) && a.findWorkDayOn(date).isEmpty()) {
                missing++;
            }
        }
        return missing;
    }

    /** Costo devengado por semana (lunes a domingo) de las ultimas semanas, incluida la actual. */
    private List<WeekCost> weeklyCost(List<Assignment> assignments, LocalDate today) {
        LocalDate currentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = CHART_WEEKS - 1; i >= 0; i--) {
            totals.put(currentWeek.minusWeeks(i), BigDecimal.ZERO);
        }
        LocalDate firstWeek = currentWeek.minusWeeks(CHART_WEEKS - 1L);
        for (Assignment a : assignments) {
            for (WorkDay day : a.getWorkDays()) {
                if (!day.getDate().isBefore(firstWeek)) {
                    LocalDate week = day.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    totals.merge(week, a.valueOf(day), BigDecimal::add);
                }
            }
        }
        BigDecimal max = totals.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return totals.entrySet().stream()
                .map(e -> new WeekCost(e.getKey(), e.getValue(), percent(e.getValue(), max)))
                .toList();
    }

    // ================================================================== trabajador

    @Override
    public WorkerDashboard workerDashboard(UUID workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("trabajador.no.encontrado"));
        LocalDate today = LocalDate.now(clock);
        List<Assignment> assignments = assignmentRepository.findByWorkerIdOrderByInvitedAtDesc(workerId);
        List<Assignment> working = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .toList();
        List<AssignmentDetail> current = working.stream().map(a -> mapper.toDetail(a, null)).toList();
        List<AssignmentRow> invitations = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.INVITED)
                .map(mapper::toRow)
                .toList();
        BigDecimal balance = working.stream().map(Assignment::calculateBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unconfirmed = working.stream().map(Assignment::calculateEarnedUnconfirmed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean arlExpiringSoon = worker.getArlValidUntil().isBefore(today.plusDays(ARL_WARNING_DAYS));

        return new WorkerDashboard(
                worker.getFirstName(),
                balance,
                unconfirmed,
                workerRepository.countConfirmedWorkDays(workerId),
                workerRepository.averageRating(workerId),
                workerRepository.countRatings(workerId),
                assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count(),
                worker.getArlValidUntil(),
                arlExpiringSoon,
                worker.isAcceptingOffers(),
                workerPending(worker, assignments, today),
                current,
                invitations);
    }

    private List<PendingItem> workerPending(Worker worker, List<Assignment> assignments, LocalDate today) {
        List<PendingItem> items = new ArrayList<>();
        for (Assignment a : assignments) {
            String link = "/trabajador/asignaciones/" + a.getId();
            String site = a.getFront().getSite().getName();
            switch (a.getStatus()) {
                case INVITED -> items.add(new PendingItem(INFO, "pendiente.responder.invitacion",
                        new Object[] {site, a.getFront().getSite().getForeman().getFullName()}, null, link,
                        "accion.responder", a.getInvitedAt()));
                case IN_PROGRESS -> {
                    long pendingDays = a.countDays(day -> day.getStatus() == WorkDayStatus.PENDING);
                    if (pendingDays > 0) {
                        items.add(new PendingItem(WARNING, "pendiente.confirmar.jornadas",
                                new Object[] {pendingDays, site}, null, link + "#jornadas", "accion.revisar", null));
                    }
                }
                case COMPLETED -> {
                    if (a.getForemanRating() == null) {
                        items.add(new PendingItem(NEUTRAL, "pendiente.calificar.maestro",
                                new Object[] {a.getFront().getSite().getForeman().getFullName()}, null,
                                link + "#calificar", "accion.calificar", null));
                    }
                }
                default -> {
                    // rechazadas y canceladas no dejan pendientes
                }
            }
            for (Payment payment : a.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    items.add(new PendingItem(INFO, "pendiente.confirmar.pago",
                            new Object[] {Formats.money(payment.getAmount()), payment.getReceiptNumber()}, null,
                            link + "#pagos", "accion.revisar", payment.getPaidAt()));
                }
            }
        }
        LocalDate arl = worker.getArlValidUntil();
        if (arl.isBefore(today)) {
            items.add(new PendingItem(DANGER, "pendiente.arl.vencida", new Object[] {Formats.date(arl)}, null,
                    "/trabajador/perfil", "accion.actualizar", null));
        } else if (arl.isBefore(today.plusDays(ARL_WARNING_DAYS))) {
            items.add(new PendingItem(WARNING, "pendiente.arl.por.vencer", new Object[] {Formats.date(arl)}, null,
                    "/trabajador/perfil", "accion.actualizar", null));
        }
        return prioritize(items);
    }

    // ================================================================== utilidades

    private static List<PendingItem> prioritize(List<PendingItem> items) {
        return items.stream()
                .sorted(Comparator.comparingInt((PendingItem item) -> PRIORITY.indexOf(item.kind()))
                        .thenComparing(PendingItem::since, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_PENDING)
                .toList();
    }

    private static int percent(BigDecimal value, BigDecimal max) {
        if (max.signum() == 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
