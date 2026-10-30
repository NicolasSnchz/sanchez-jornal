package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jornal.config.LaborProperties;
import com.jornal.dto.form.InvitationForm;
import com.jornal.dto.view.CandidateRow;
import com.jornal.entity.Assignment;
import com.jornal.entity.AssignmentStatus;
import com.jornal.entity.Trade;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.exception.BusinessRuleException;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.WorkFrontRepository;
import com.jornal.repository.WorkerRepository;
import com.jornal.service.impl.AssignmentServiceImpl;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Pruebas del servicio con Mockito. Solo se simula la frontera con la base de
 * datos (repositorios); el dominio, el mapper y la politica de jornal son reales,
 * asi la prueba verifica el comportamiento y no detalles de implementacion.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    private static final UUID FOREMAN_ID = UUID.randomUUID();

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private WorkFrontRepository frontRepository;
    @Mock
    private WorkerRepository workerRepository;

    private AssignmentServiceImpl service;
    private WorkFront front;

    @BeforeEach
    void setUp() {
        Clock clock = Clocks.fixedAtTestNow();
        WagePolicy wagePolicy = new WagePolicy(new LaborProperties(new BigDecimal("1750905"), 210));
        service = new AssignmentServiceImpl(assignmentRepository, frontRepository, workerRepository,
                new ViewMapper(clock), wagePolicy, clock);
        front = TestData.front(TestData.siteInProgress(TestData.foreman()));
    }

    @Test
    void invite_trabajadorApto_guardaLaInvitacionYOcupaUnCupo() {
        Worker worker = TestData.worker();
        givenFrontAndWorker(worker);

        UUID id = service.invite(FOREMAN_ID, front.getId(), invitation(worker, "80000"));

        assertThat(id).isNotNull();
        assertThat(front.availableOpenings()).isEqualTo(2);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void invite_arlVenceAntesDelFrente_noGuardaYExplicaElMotivo() {
        Worker worker = TestData.worker(Trade.HELPER, front.getEndDate().minusDays(1));
        givenFrontAndWorker(worker);

        assertThatThrownBy(() -> service.invite(FOREMAN_ID, front.getId(), invitation(worker, "80000")))
                .extracting("code").isEqualTo("elegibilidad.arl.vence.antes");
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void invite_jornalBajoElMinimoLegal_esRechazadoConElMinimo() {
        Worker worker = TestData.worker();
        givenFrontAndWorker(worker);

        // 8 horas diarias: SMMLV 1.750.905 / 210 h = 8.337,64 por hora -> 66.702 por dia
        assertThatThrownBy(() -> service.invite(FOREMAN_ID, front.getId(), invitation(worker, "60000")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> {
                    BusinessRuleException rule = (BusinessRuleException) e;
                    assertThat(rule.getCode()).isEqualTo("invitacion.jornal.bajo.minimo");
                    assertThat(rule.getArgs()).containsExactly(new BigDecimal("66702"));
                });
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void invite_trabajadorOcupadoEnOtraObra_esRechazado() {
        Worker worker = TestData.worker();
        givenFrontAndWorker(worker);
        when(assignmentRepository.existsInProgressOverlapping(eq(worker.getId()), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.invite(FOREMAN_ID, front.getId(), invitation(worker, "80000")))
                .extracting("code").isEqualTo("elegibilidad.agenda.cruzada");
    }

    @Test
    void invite_frenteDeOtroMaestro_respondeComoSiNoExistiera() {
        when(frontRepository.findByIdAndSiteForemanId(front.getId(), FOREMAN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.invite(FOREMAN_ID, front.getId(), invitation(TestData.worker(), "80000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code").isEqualTo("frente.no.encontrado");
    }

    @Test
    void findCandidates_incluyeLosNoAptosConSuMotivo() {
        Worker apto = TestData.worker();
        Worker sinArl = TestData.worker(Trade.HELPER, LocalDate.of(2026, 11, 1));
        when(frontRepository.findByIdAndSiteForemanId(front.getId(), FOREMAN_ID)).thenReturn(Optional.of(front));
        when(workerRepository.searchCandidates(eq(Trade.HELPER), eq("C\u00facuta"), eq("yei"), any(Pageable.class)))
                .thenReturn(List.of(apto, sinArl));

        List<CandidateRow> candidates = service.findCandidates(FOREMAN_ID, front.getId(), "  yei ");

        assertThat(candidates).extracting(CandidateRow::eligible).containsExactly(true, false);
        assertThat(candidates.get(1).issues()).containsExactly("elegibilidad.arl.vence.antes");
    }

    @Test
    void findCandidates_quienYaEstaEnElFrenteNoApareceConCruceDeAgenda() {
        Worker enElFrente = TestData.worker();
        when(frontRepository.findByIdAndSiteForemanId(front.getId(), FOREMAN_ID)).thenReturn(Optional.of(front));
        when(workerRepository.searchCandidates(eq(Trade.HELPER), eq("C\u00facuta"), eq(""), any(Pageable.class)))
                .thenReturn(List.of(enElFrente));
        when(assignmentRepository.existsByWorkerIdAndFrontIdAndStatusIn(eq(enElFrente.getId()), eq(front.getId()), any()))
                .thenReturn(true);
        lenient().when(assignmentRepository.existsInProgressOverlapping(eq(enElFrente.getId()), any(), any()))
                .thenReturn(true);

        List<CandidateRow> candidates = service.findCandidates(FOREMAN_ID, front.getId(), null);

        // Su propia asignacion se cruza con las fechas del frente, pero el unico motivo es que ya esta en el.
        assertThat(candidates.get(0).issues()).containsExactly("elegibilidad.ya.invitado");
    }

    @Test
    void respond_aceptarCuandoYaTrabajaEnEsasFechas_esRechazado() {
        Worker worker = TestData.worker();
        Assignment invited = Assignment.invite(front, worker, TestData.WAGE, TestData.TODAY, TestData.NOW);
        when(assignmentRepository.findByIdAndWorkerId(invited.getId(), worker.getId())).thenReturn(Optional.of(invited));
        when(assignmentRepository.existsInProgressOverlapping(eq(worker.getId()), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.respond(worker.getId(), invited.getId(), true))
                .extracting("code").isEqualTo("asignacion.aceptar.agenda.cruzada");
        assertThat(invited.getStatus()).isEqualTo(AssignmentStatus.INVITED);
    }

    @Test
    void respond_rechazar_liberaElCupo() {
        Worker worker = TestData.worker();
        Assignment invited = Assignment.invite(front, worker, TestData.WAGE, TestData.TODAY, TestData.NOW);
        when(assignmentRepository.findByIdAndWorkerId(invited.getId(), worker.getId())).thenReturn(Optional.of(invited));

        service.respond(worker.getId(), invited.getId(), false);

        assertThat(invited.getStatus()).isEqualTo(AssignmentStatus.REJECTED);
        assertThat(front.availableOpenings()).isEqualTo(3);
    }

    private void givenFrontAndWorker(Worker worker) {
        when(frontRepository.findByIdAndSiteForemanId(front.getId(), FOREMAN_ID)).thenReturn(Optional.of(front));
        when(workerRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(assignmentRepository.existsByWorkerIdAndFrontIdAndStatusIn(eq(worker.getId()), eq(front.getId()),
                anyCollection())).thenReturn(false);
    }

    private static InvitationForm invitation(Worker worker, String wage) {
        InvitationForm form = new InvitationForm();
        form.setWorkerId(worker.getId());
        form.setDailyWage(new BigDecimal(wage));
        form.setStartDate(TestData.TODAY);
        return form;
    }

}
