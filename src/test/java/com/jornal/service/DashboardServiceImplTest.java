package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jornal.dto.view.ForemanDashboard;
import com.jornal.dto.view.PendingItem;
import com.jornal.dto.view.WorkerDashboard;
import com.jornal.entity.Advance;
import com.jornal.entity.Assignment;
import com.jornal.entity.Foreman;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.Trade;
import com.jornal.entity.WorkDay;
import com.jornal.entity.Worker;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.ForemanRepository;
import com.jornal.repository.PaymentRepository;
import com.jornal.repository.WorkerRepository;
import com.jornal.service.impl.DashboardServiceImpl;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceImplTest {

    @Mock
    private ForemanRepository foremanRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private ConstructionSiteRepository siteRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private DashboardServiceImpl service;
    private Assignment assignment;
    private Foreman foreman;
    private Worker worker;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(foremanRepository, workerRepository, siteRepository, assignmentRepository,
                paymentRepository, new ViewMapper(Clocks.fixedAtTestNow()), Clocks.fixedAtTestNow());
        assignment = TestData.assignmentInProgress();
        foreman = assignment.getFront().getSite().getForeman();
        worker = assignment.getWorker();
        // Lunes y martes confirmados; el miercoles objetado; el jueves (hoy) aun no termina.
        TestData.confirmedDay(assignment, TestData.MONDAY);
        TestData.confirmedDay(assignment, TestData.MONDAY.plusDays(1));
        WorkDay wednesday = assignment.registerWorkDay(TestData.MONDAY.plusDays(2), TestData.fullDay(),
                TestData.TODAY, TestData.NOW);
        wednesday.dispute("Sali a las 5:00 p. m.", TestData.NOW);
        new Advance(assignment, "JOR-000001", new BigDecimal("40000"), PaymentMethod.CASH, null, TestData.NOW,
                "Botas", true);

        when(foremanRepository.findById(foreman.getId())).thenReturn(Optional.of(foreman));
        when(siteRepository.findByForemanIdOrderByCreatedAtDesc(foreman.getId()))
                .thenReturn(List.of(assignment.getFront().getSite()));
        when(assignmentRepository.findByFrontSiteForemanIdAndStatusInOrderByInvitedAtDesc(eq(foreman.getId()),
                anyCollection())).thenReturn(List.of(assignment));
        when(paymentRepository.findByForemanBetween(eq(foreman.getId()), any(), any()))
                .thenReturn(List.copyOf(assignment.getPayments()));
        when(workerRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(assignmentRepository.findByWorkerIdOrderByInvitedAtDesc(worker.getId())).thenReturn(List.of(assignment));
    }

    @Test
    void foremanDashboard_laJornadaObjetadaEsElPrimerPendienteYLlevaACorregirla() {
        ForemanDashboard dashboard = service.foremanDashboard(foreman.getId());

        PendingItem first = dashboard.pending().get(0);
        assertThat(first.kind()).isEqualTo("danger");
        assertThat(first.messageCode()).isEqualTo("pendiente.jornada.objetada");
        assertThat(first.link()).endsWith("/editar");
        assertThat(first.detail()).isEqualTo("Sali a las 5:00 p. m.");
    }

    @Test
    void foremanDashboard_resumeSaldoPorPagarYCostoSemanal() {
        ForemanDashboard dashboard = service.foremanDashboard(foreman.getId());

        assertThat(dashboard.pending()).extracting(PendingItem::messageCode)
                .contains("pendiente.saldo.por.pagar");
        assertThat(dashboard.workersInProgress()).isEqualTo(1);
        assertThat(dashboard.monthSpending()).isEqualByComparingTo("40000");
        assertThat(dashboard.weeklyCost()).hasSize(6);
        assertThat(dashboard.weeklyCost().get(5).total()).isEqualByComparingTo("240000");
        assertThat(dashboard.weeklyCost().get(5).percentOfMax()).isEqualTo(100);
    }

    @Test
    void workerDashboard_muestraLoQueLeDebenYLoQueDebeConfirmar() {
        WorkerDashboard dashboard = service.workerDashboard(worker.getId());

        assertThat(dashboard.balance()).isEqualByComparingTo("120000");
        assertThat(dashboard.unconfirmed()).isEqualByComparingTo("80000");
        assertThat(dashboard.pending()).extracting(PendingItem::messageCode)
                .contains("pendiente.confirmar.pago");
        assertThat(dashboard.current()).hasSize(1);
    }

    @Test
    void workerDashboard_arlProximaAVencer_avisaAlTrabajador() {
        Worker soonExpiring = TestData.worker(Trade.HELPER, LocalDate.of(2026, 11, 10));
        when(workerRepository.findById(soonExpiring.getId())).thenReturn(Optional.of(soonExpiring));

        WorkerDashboard dashboard = service.workerDashboard(soonExpiring.getId());

        assertThat(dashboard.arlExpiringSoon()).isTrue();
        assertThat(dashboard.pending()).extracting(PendingItem::messageCode).containsExactly("pendiente.arl.por.vencer");
    }
}
