package com.jornal.service.impl;

import com.jornal.dto.form.PaymentForm;
import com.jornal.dto.view.PaymentRow;
import com.jornal.dto.view.PaymentsMonth;
import com.jornal.dto.view.Receipt;
import com.jornal.entity.Advance;
import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentStatus;
import com.jornal.entity.PaymentType;
import com.jornal.entity.Settlement;
import com.jornal.entity.Worker;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.PaymentRepository;
import com.jornal.service.PaymentService;
import com.jornal.service.ViewMapper;
import com.jornal.strategy.PaymentCommand;
import com.jornal.strategy.PaymentQuote;
import com.jornal.strategy.PaymentStrategy;
import com.jornal.util.AmountInWords;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra pagos delegando las reglas en una estrategia por tipo (patron
 * Strategy). Agregar un tipo nuevo de pago es crear otra clase que implemente
 * {@link PaymentStrategy}: este servicio no cambia (principio abierto/cerrado).
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final Map<PaymentType, PaymentStrategy> strategies;
    private final AssignmentRepository assignmentRepository;
    private final PaymentRepository paymentRepository;
    private final ViewMapper mapper;
    private final Clock clock;

    public PaymentServiceImpl(List<PaymentStrategy> strategies, AssignmentRepository assignmentRepository,
                              PaymentRepository paymentRepository, ViewMapper mapper, Clock clock) {
        this.strategies = indexByType(strategies);
        this.assignmentRepository = assignmentRepository;
        this.paymentRepository = paymentRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Falla al arrancar si falta una estrategia o hay dos para el mismo tipo. */
    static Map<PaymentType, PaymentStrategy> indexByType(List<PaymentStrategy> strategies) {
        Map<PaymentType, PaymentStrategy> byType = new EnumMap<>(PaymentType.class);
        for (PaymentStrategy strategy : strategies) {
            if (byType.put(strategy.type(), strategy) != null) {
                throw new IllegalStateException("Hay dos estrategias para " + strategy.type());
            }
        }
        for (PaymentType type : PaymentType.values()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException("Falta la estrategia de pago para " + type);
            }
        }
        return byType;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQuote quote(UUID foremanId, UUID assignmentId, PaymentForm form) {
        Assignment assignment = assignmentRepository.findByIdAndFrontSiteForemanId(assignmentId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("asignacion.no.encontrada"));
        return strategies.get(form.getType()).quote(assignment, form.toCommand(), LocalDate.now(clock));
    }

    @Override
    public PaymentRow register(UUID foremanId, UUID assignmentId, PaymentForm form) {
        Assignment assignment = assignmentRepository.findForPayment(assignmentId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("asignacion.no.encontrada"));
        PaymentStrategy strategy = strategies.get(form.getType());
        PaymentCommand command = form.toCommand();
        LocalDateTime now = LocalDateTime.now(clock);
        // Se valida antes de pedir el consecutivo, para no gastar numeros de comprobante en pagos rechazados.
        strategy.quote(assignment, command, now.toLocalDate());
        String receiptNumber = "JOR-%06d".formatted(paymentRepository.nextReceiptSequence());
        Payment payment = strategy.create(assignment, command, receiptNumber, now);
        paymentRepository.save(payment);
        log.info("Pago {} ({}) registrado en la asignacion {}", receiptNumber, payment.getType(), assignmentId);
        return mapper.toRow(payment);
    }

    @Override
    public UUID cancel(UUID foremanId, UUID paymentId, String reason) {
        Payment payment = paymentRepository.findByIdAndAssignmentFrontSiteForemanId(paymentId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("pago.no.encontrado"));
        payment.cancel(reason, LocalDateTime.now(clock));
        log.info("Pago {} anulado", payment.getReceiptNumber());
        return payment.getAssignment().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentsMonth listMonth(UUID foremanId, YearMonth month) {
        List<Payment> payments = paymentRepository.findByForemanBetween(foremanId,
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        List<Payment> effective = payments.stream().filter(Payment::isEffective).toList();
        Predicate<Payment> advance = p -> p.getType() == PaymentType.ADVANCE;
        YearMonth current = YearMonth.now(clock);
        return new PaymentsMonth(month, month.minusMonths(1), month.plusMonths(1), month.isBefore(current),
                sum(effective, p -> true),
                sum(effective, advance), effective.stream().filter(advance).count(),
                sum(effective, advance.negate()), effective.stream().filter(advance.negate()).count(),
                payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count(),
                payments.stream().filter(p -> p.getStatus() == PaymentStatus.CANCELLED).count(),
                payments.stream().map(mapper::toRow).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Receipt receiptForForeman(UUID foremanId, UUID paymentId) {
        return toReceipt(paymentRepository.findByIdAndAssignmentFrontSiteForemanId(paymentId, foremanId)
                .orElseThrow(() -> new ResourceNotFoundException("pago.no.encontrado")));
    }

    @Override
    public UUID confirmReceipt(UUID workerId, UUID paymentId) {
        Payment payment = findForWorker(workerId, paymentId);
        payment.confirmReceipt(LocalDateTime.now(clock));
        log.info("Pago {} confirmado por el trabajador", payment.getReceiptNumber());
        return payment.getAssignment().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Receipt receiptForWorker(UUID workerId, UUID paymentId) {
        return toReceipt(findForWorker(workerId, paymentId));
    }

    private Payment findForWorker(UUID workerId, UUID paymentId) {
        return paymentRepository.findByIdAndAssignmentWorkerId(paymentId, workerId)
                .orElseThrow(() -> new ResourceNotFoundException("pago.no.encontrado"));
    }

    /**
     * En una liquidacion: devengado del periodo - anticipos entregados en el
     * periodo = neto pagado. Las jornadas del periodo ya no se pueden editar, asi
     * que el desglose siempre da el mismo resultado.
     */
    private Receipt toReceipt(Payment payment) {
        Assignment assignment = payment.getAssignment();
        Worker worker = assignment.getWorker();
        ConstructionSite site = assignment.getFront().getSite();
        Foreman foreman = site.getForeman();
        String reason = payment instanceof Advance advance ? advance.getReason() : null;
        LocalDate periodStart = null;
        LocalDate periodEnd = null;
        long daysInPeriod = 0;
        BigDecimal earnedInPeriod = null;
        BigDecimal discounted = null;
        if (payment instanceof Settlement settlement) {
            periodStart = settlement.getPeriodStart();
            periodEnd = settlement.getPeriodEnd();
            daysInPeriod = assignment.countDays(day -> settlement.includes(day.getDate()));
            earnedInPeriod = assignment.calculateEarnedConfirmedUntil(periodEnd)
                    .subtract(assignment.calculateEarnedConfirmedUntil(periodStart.minusDays(1)));
            discounted = earnedInPeriod.subtract(payment.getAmount());
        }
        return new Receipt(payment.getId(), assignment.getId(), payment.getReceiptNumber(), payment.getType(),
                payment.getStatus(), payment.getPaidAt(), payment.getAmount(),
                AmountInWords.pesos(payment.getAmount()), payment.getMethod(), payment.getTransactionReference(),
                reason, periodStart, periodEnd, daysInPeriod, earnedInPeriod, discounted,
                payment.getConfirmedAt(), payment.getCancelledAt(), payment.getCancellationReason(),
                foreman.getFullName(), foreman.getCompanyName(), foreman.getDocumentType(),
                foreman.getDocumentNumber(), worker.getFullName(), worker.getDocumentType(),
                worker.getDocumentNumber(), site.getName(), site.getAddress(), site.getCity(),
                assignment.getFront().getTask(), assignment.getDailyWage());
    }

    private static BigDecimal sum(List<Payment> payments, Predicate<Payment> condition) {
        return payments.stream().filter(condition).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
