package com.jornal.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jornal.entity.Advance;
import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Foreman;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.Trade;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Consultas contra una base real (H2 en modo PostgreSQL) creada por las
 * migraciones de Flyway. Que el contexto arranque ya prueba que el esquema SQL y
 * el mapeo JPA coinciden (ddl-auto=validate).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryQueriesTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private ConstructionSiteRepository siteRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    private Foreman owner;
    private Foreman otherForeman;
    private Worker worker;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        owner = em.persist(TestData.foreman());
        otherForeman = em.persist(TestData.foreman());
        worker = em.persist(TestData.worker());
        ConstructionSite site = TestData.siteInProgress(owner);
        WorkFront front = TestData.front(site);
        em.persist(site);
        assignment = Assignment.invite(front, worker, TestData.WAGE, LocalDate.of(2026, 10, 5),
                LocalDateTime.of(2026, 10, 2, 17, 0));
        assignment.accept(LocalDateTime.of(2026, 10, 2, 19, 0));
        TestData.confirmedDay(assignment, LocalDate.of(2026, 10, 5));
        em.persist(assignment);
        em.flush();
        em.clear();
    }

    @Test
    void findByIdAndFrontSiteForemanId_soloLoEncuentraElDuenoDeLaObra() {
        assertThat(assignmentRepository.findByIdAndFrontSiteForemanId(assignment.getId(), owner.getId())).isPresent();
        assertThat(assignmentRepository.findByIdAndFrontSiteForemanId(assignment.getId(), otherForeman.getId()))
                .isEmpty();
    }

    @Test
    void findByIdAndWorkerId_otroTrabajadorNoLoVe() {
        assertThat(assignmentRepository.findByIdAndWorkerId(assignment.getId(), worker.getId())).isPresent();
        assertThat(assignmentRepository.findByIdAndWorkerId(assignment.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void existsInProgressOverlapping_detectaCrucesDeFechas() {
        // La asignacion ocupa del 5 de octubre al 18 de diciembre (fin del frente).
        assertThat(assignmentRepository.existsInProgressOverlapping(worker.getId(),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(assignmentRepository.existsInProgressOverlapping(worker.getId(),
                LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 31))).isFalse();
    }

    @Test
    void searchCandidates_filtraPorOficioCiudadYDisponibilidad() {
        Worker welder = em.persist(TestData.worker(Trade.WELDER, TestData.TODAY.plusYears(1)));
        Worker unavailable = TestData.worker();
        unavailable.changeAvailability(false);
        em.persist(unavailable);
        em.flush();

        var found = workerRepository.searchCandidates(Trade.HELPER, "c\u00faCUTA", "", PageRequest.of(0, 10));

        assertThat(found).extracting(Worker::getId).contains(worker.getId())
                .doesNotContain(welder.getId(), unavailable.getId());
    }

    @Test
    void searchCandidates_textoConInyeccionSql_seTrataComoUnNombreLiteral() {
        var found = workerRepository.searchCandidates(Trade.HELPER, "C\u00facuta", "' OR '1'='1",
                PageRequest.of(0, 10));

        assertThat(found).isEmpty();
        assertThat(workerRepository.count()).isPositive();
    }

    @Test
    void countConfirmedWorkDays_cuentaSoloLasConfirmadas() {
        assertThat(workerRepository.countConfirmedWorkDays(worker.getId())).isEqualTo(1);
    }

    @Test
    void nextReceiptSequence_entregaNumerosConsecutivos() {
        long first = paymentRepository.nextReceiptSequence();

        assertThat(paymentRepository.nextReceiptSequence()).isEqualTo(first + 1);
    }

    @Test
    void findByForemanBetween_soloPagosDelMaestroEnElRango() {
        Assignment managed = assignmentRepository.findById(assignment.getId()).orElseThrow();
        em.persist(new Advance(managed, "JOR-900001", new BigDecimal("30000"), PaymentMethod.CASH, null,
                LocalDateTime.of(2026, 10, 6, 18, 0), "Transporte", true));
        em.flush();

        assertThat(paymentRepository.findByForemanBetween(owner.getId(), LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0))).hasSize(1);
        assertThat(paymentRepository.findByForemanBetween(otherForeman.getId(), LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0))).isEmpty();
        assertThat(paymentRepository.findByForemanBetween(owner.getId(), LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0))).isEmpty();
    }

    @Test
    void findByForemanIdOrderByCreatedAtDesc_noMezclaObrasDeOtrosMaestros() {
        assertThat(siteRepository.findByForemanIdOrderByCreatedAtDesc(owner.getId())).hasSize(1);
        assertThat(siteRepository.findByForemanIdOrderByCreatedAtDesc(otherForeman.getId())).isEmpty();
    }
}
