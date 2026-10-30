package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jornal.dto.form.PaymentForm;
import com.jornal.dto.view.PaymentRow;
import com.jornal.dto.view.PaymentsMonth;
import com.jornal.dto.view.Receipt;
import com.jornal.entity.Advance;
import com.jornal.entity.Assignment;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.PaymentRepository;
import com.jornal.service.impl.PaymentServiceImpl;
import com.jornal.strategy.AdvancePaymentStrategy;
import com.jornal.strategy.FinalSettlementStrategy;
import com.jornal.strategy.PaymentStrategy;
import com.jornal.strategy.PeriodSettlementStrategy;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final UUID FOREMAN_ID = UUID.randomUUID();
    private static final LocalDate W2_MON = LocalDate.of(2026, 10, 19);

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private PaymentServiceImpl service;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        Clock clock = Clocks.fixedAtTestNow();
        List<PaymentStrategy> strategies = List.of(new AdvancePaymentStrategy(), new PeriodSettlementStrategy(),
                new FinalSettlementStrategy());
        service = new PaymentServiceImpl(strategies, assignmentRepository, paymentRepository, new ViewMapper(clock),
                clock);
        assignment = TestData.assignmentInProgress();
        for (LocalDate d = W2_MON; !d.isAfter(W2_MON.plusDays(4)); d = d.plusDays(1)) {
            TestData.confirmedDay(assignment, d);
        }
    }

    @Test
    void constructor_faltaUnaEstrategia_fallaAlArrancar() {
        List<PaymentStrategy> incomplete = List.of(new AdvancePaymentStrategy(), new PeriodSettlementStrategy());

        assertThatThrownBy(() -> new PaymentServiceImpl(incomplete, assignmentRepository, paymentRepository, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FINAL_SETTLEMENT");
    }

    @Test
    void constructor_dosEstrategiasParaElMismoTipo_fallaAlArrancar() {
        List<PaymentStrategy> duplicated = List.of(new AdvancePaymentStrategy(), new AdvancePaymentStrategy());

        assertThatThrownBy(() -> new PaymentServiceImpl(duplicated, assignmentRepository, paymentRepository, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void register_liquidacion_usaLaEstrategiaYUnConsecutivoConFormato() {
        when(assignmentRepository.findForPayment(assignment.getId(), FOREMAN_ID)).thenReturn(Optional.of(assignment));
        when(paymentRepository.nextReceiptSequence()).thenReturn(42L);

        PaymentRow row = service.register(FOREMAN_ID, assignment.getId(), settlementUntil(W2_MON.plusDays(4)));

        assertThat(row.receiptNumber()).isEqualTo("JOR-000042");
        assertThat(row.type()).isEqualTo(PaymentType.SETTLEMENT);
        assertThat(row.amount()).isEqualByComparingTo("400000");
        assertThat(assignment.calculateBalance()).isZero();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void register_pagoQueNoCumpleLasReglas_noGastaUnNumeroDeComprobante() {
        assignment.registerWorkDay(LocalDate.of(2026, 10, 26), TestData.fullDay(), TestData.TODAY, TestData.NOW);
        when(assignmentRepository.findForPayment(assignment.getId(), FOREMAN_ID)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.register(FOREMAN_ID, assignment.getId(),
                settlementUntil(LocalDate.of(2026, 10, 26))))
                .extracting("code").isEqualTo("pago.liquidacion.jornadas.sin.confirmar");
        verify(paymentRepository, never()).nextReceiptSequence();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void quote_noRegistraNada() {
        when(assignmentRepository.findByIdAndFrontSiteForemanId(assignment.getId(), FOREMAN_ID))
                .thenReturn(Optional.of(assignment));

        BigDecimal amount = service.quote(FOREMAN_ID, assignment.getId(), finalSettlement()).amount();

        assertThat(amount).isEqualByComparingTo("400000");
        assertThat(assignment.getPayments()).isEmpty();
    }

    @Test
    void receiptForWorker_pagoDeOtroTrabajador_noEncontrado() {
        UUID paymentId = UUID.randomUUID();
        UUID otherWorker = UUID.randomUUID();
        when(paymentRepository.findByIdAndAssignmentWorkerId(paymentId, otherWorker)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receiptForWorker(otherWorker, paymentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void receiptForForeman_liquidacionConAnticipo_desglosaElNetoYLoEscribeEnLetras() {
        Advance advance = new Advance(assignment, "JOR-000001", new BigDecimal("50000"), PaymentMethod.CASH, null,
                W2_MON.atTime(18, 0), "Transporte", true);
        advance.confirmReceipt(W2_MON.atTime(18, 30));
        Payment settlement = new PeriodSettlementStrategy().create(assignment, settlementUntil(W2_MON.plusDays(4))
                .toCommand(), "JOR-000002", W2_MON.plusDays(5).atTime(10, 0));
        when(paymentRepository.findByIdAndAssignmentFrontSiteForemanId(settlement.getId(), FOREMAN_ID))
                .thenReturn(Optional.of(settlement));

        Receipt receipt = service.receiptForForeman(FOREMAN_ID, settlement.getId());

        assertThat(receipt.daysInPeriod()).isEqualTo(5);
        assertThat(receipt.earnedInPeriod()).isEqualByComparingTo("400000");
        assertThat(receipt.discountedAdvances()).isEqualByComparingTo("50000");
        assertThat(receipt.amount()).isEqualByComparingTo("350000");
        assertThat(receipt.amountInWords()).isEqualTo("trescientos cincuenta mil pesos");
    }

    @Test
    void listMonth_losTotalesNoIncluyenPagosAnulados() {
        Advance kept = new Advance(assignment, "JOR-000001", new BigDecimal("50000"), PaymentMethod.CASH, null,
                W2_MON.atTime(18, 0), "Transporte", true);
        Advance cancelled = new Advance(assignment, "JOR-000002", new BigDecimal("30000"), PaymentMethod.CASH, null,
                W2_MON.atTime(19, 0), "Almuerzo", false);
        cancelled.cancel("Duplicado", W2_MON.atTime(19, 5));
        when(paymentRepository.findByForemanBetween(any(), any(), any())).thenReturn(List.of(cancelled, kept));

        PaymentsMonth month = service.listMonth(FOREMAN_ID, YearMonth.of(2026, 10));

        assertThat(month.total()).isEqualByComparingTo("50000");
        assertThat(month.advancesCount()).isEqualTo(1);
        assertThat(month.cancelledCount()).isEqualTo(1);
        assertThat(month.pendingConfirmation()).isEqualTo(1);
        assertThat(month.payments()).hasSize(2);
        assertThat(month.hasNext()).isFalse();
    }

    private static PaymentForm settlementUntil(LocalDate end) {
        PaymentForm form = new PaymentForm();
        form.setType(PaymentType.SETTLEMENT);
        form.setMethod(PaymentMethod.CASH);
        form.setPeriodEnd(end);
        return form;
    }

    private static PaymentForm finalSettlement() {
        PaymentForm form = new PaymentForm();
        form.setType(PaymentType.FINAL_SETTLEMENT);
        form.setMethod(PaymentMethod.CASH);
        return form;
    }
}
