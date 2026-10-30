package com.jornal.entity;

import static com.jornal.support.TestData.TODAY;
import static com.jornal.support.TestData.WAGE;
import static com.jornal.support.TestData.confirmedDay;
import static com.jornal.support.TestData.fullDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.exception.BusinessRuleException;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssignmentTest {

    private static final LocalDate MON = LocalDate.of(2026, 10, 19);
    private static final LocalDate TUE = MON.plusDays(1);
    private static final LocalDate WED = MON.plusDays(2);

    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignment = TestData.assignmentInProgress();
    }

    @Nested
    class Invitacion {

        private WorkFront front;

        @BeforeEach
        void setUp() {
            front = TestData.front(TestData.siteInProgress(TestData.foreman()));
        }

        @Test
        void invite_datosValidos_quedaInvitadoYOcupaCupo() {
            Assignment invited = Assignment.invite(front, TestData.worker(), WAGE, TODAY, TestData.NOW);

            assertThat(invited.getStatus()).isEqualTo(AssignmentStatus.INVITED);
            assertThat(front.availableOpenings()).isEqualTo(2);
        }

        @Test
        void invite_obraSuspendida_esRechazada() {
            front.getSite().suspend();

            assertThatThrownBy(() -> Assignment.invite(front, TestData.worker(), WAGE, TODAY, TestData.NOW))
                    .extracting("code").isEqualTo("invitacion.obra.no.admite");
        }

        @Test
        void invite_sinCupos_esRechazada() {
            for (int i = 0; i < 3; i++) {
                Assignment.invite(front, TestData.worker(), WAGE, TODAY, TestData.NOW);
            }

            assertThatThrownBy(() -> Assignment.invite(front, TestData.worker(), WAGE, TODAY, TestData.NOW))
                    .extracting("code").isEqualTo("invitacion.sin.cupos");
        }

        @Test
        void invite_inicioEnElPasado_esRechazada() {
            assertThatThrownBy(() -> Assignment.invite(front, TestData.worker(), WAGE, TODAY.minusDays(1), TestData.NOW))
                    .extracting("code").isEqualTo("invitacion.inicio.invalido");
        }

        @Test
        void invite_inicioDespuesDelFinDelFrente_esRechazada() {
            assertThatThrownBy(() -> Assignment.invite(front, TestData.worker(), WAGE, LocalDate.of(2027, 1, 4),
                    TestData.NOW)).extracting("code").isEqualTo("invitacion.inicio.invalido");
        }

        @Test
        void accept_invitacionYaRespondida_esRechazada() {
            Assignment invited = Assignment.invite(front, TestData.worker(), WAGE, TODAY, TestData.NOW);
            invited.reject(TestData.NOW);

            assertThatThrownBy(() -> invited.accept(TestData.NOW))
                    .extracting("code").isEqualTo("asignacion.responder.estado.invalido");
        }
    }

    @Nested
    class RegistroDeJornadas {

        @Test
        void registerWorkDay_diaValido_quedaPorConfirmar() {
            WorkDay day = assignment.registerWorkDay(TUE, fullDay(), TODAY, TestData.NOW);

            assertThat(day.getStatus()).isEqualTo(WorkDayStatus.PENDING);
            assertThat(assignment.findWorkDayOn(TUE)).contains(day);
        }

        @Test
        void registerWorkDay_fechaFutura_esRechazada() {
            assertThatThrownBy(() -> assignment.registerWorkDay(TODAY.plusDays(1), fullDay(), TODAY, TestData.NOW))
                    .extracting("code").isEqualTo("jornada.fecha.futura");
        }

        @Test
        void registerWorkDay_antesDelInicio_esRechazada() {
            assertThatThrownBy(() -> assignment.registerWorkDay(LocalDate.of(2026, 10, 2), fullDay(), TODAY,
                    TestData.NOW)).extracting("code").isEqualTo("jornada.fecha.fuera.de.periodo");
        }

        @Test
        void registerWorkDay_sabado_esRechazadaPorqueNoEsDiaLaboral() {
            assertThatThrownBy(() -> assignment.registerWorkDay(LocalDate.of(2026, 10, 24), fullDay(), TODAY,
                    TestData.NOW)).extracting("code").isEqualTo("jornada.dia.no.laboral");
        }

        @Test
        void registerWorkDay_festivo_esRechazadaConElNombreDelFestivo() {
            LocalDate diaDeLaRaza = LocalDate.of(2026, 10, 12);

            assertThatThrownBy(() -> assignment.registerWorkDay(diaDeLaRaza, fullDay(), TODAY, TestData.NOW))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(e -> {
                        BusinessRuleException rule = (BusinessRuleException) e;
                        assertThat(rule.getCode()).isEqualTo("jornada.festivo");
                        assertThat(rule.getArgs()).contains("D\u00eda de la Raza");
                    });
        }

        @Test
        void registerWorkDay_mismoDiaDosVeces_esRechazada() {
            assignment.registerWorkDay(TUE, fullDay(), TODAY, TestData.NOW);

            assertThatThrownBy(() -> assignment.registerWorkDay(TUE, fullDay(), TODAY, TestData.NOW))
                    .extracting("code").isEqualTo("jornada.duplicada");
        }

        @Test
        void registerWorkDay_obraSuspendida_esRechazada() {
            assignment.getFront().getSite().suspend();

            assertThatThrownBy(() -> assignment.registerWorkDay(TUE, fullDay(), TODAY, TestData.NOW))
                    .extracting("code").isEqualTo("jornada.obra.no.en.curso");
        }

        @Test
        void registerWorkDay_asignacionSoloInvitada_esRechazada() {
            WorkFront front = TestData.front(TestData.siteInProgress(TestData.foreman()));
            Assignment invited = Assignment.invite(front, TestData.worker(), WAGE, TODAY, TestData.NOW);

            assertThatThrownBy(() -> invited.registerWorkDay(TODAY, fullDay(), TODAY, TestData.NOW))
                    .extracting("code").isEqualTo("jornada.asignacion.no.en.curso");
        }
    }

    @Nested
    class Saldo {

        @Test
        void calculateBalance_soloCuentaJornadasConfirmadasMenosPagos() {
            confirmedDay(assignment, MON);
            confirmedDay(assignment, TUE);
            assignment.registerWorkDay(WED, fullDay(), TODAY, TestData.NOW);
            new Advance(assignment, "JOR-000001", new BigDecimal("50000"), PaymentMethod.CASH, null,
                    TestData.NOW, "Transporte", true);

            assertThat(assignment.calculateEarnedConfirmed()).isEqualByComparingTo("160000");
            assertThat(assignment.calculateEarnedUnconfirmed()).isEqualByComparingTo("80000");
            assertThat(assignment.calculatePaidTotal()).isEqualByComparingTo("50000");
            assertThat(assignment.calculateBalance()).isEqualByComparingTo("110000");
        }

        @Test
        void calculatePaidTotal_ignoraPagosAnulados() {
            confirmedDay(assignment, MON);
            Advance advance = new Advance(assignment, "JOR-000001", new BigDecimal("50000"), PaymentMethod.CASH,
                    null, TestData.NOW, "Transporte", true);

            advance.cancel("No se entrego", TestData.NOW);

            assertThat(assignment.calculatePaidTotal()).isZero();
            assertThat(assignment.calculateBalance()).isEqualByComparingTo("80000");
        }
    }

    @Nested
    class Cierre {

        @Test
        void cancel_conJornadasRegistradas_esRechazada() {
            assignment.registerWorkDay(MON, fullDay(), TODAY, TestData.NOW);

            assertThatThrownBy(() -> assignment.cancel("Ya no lo necesito", TestData.NOW))
                    .extracting("code").isEqualTo("asignacion.cancelar.con.jornadas");
        }

        @Test
        void cancel_sinJornadas_liberaElCupo() {
            int before = assignment.getFront().availableOpenings();

            assignment.cancel("Se cayo el contrato", TestData.NOW);

            assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.CANCELLED);
            assertThat(assignment.getFront().availableOpenings()).isEqualTo(before + 1);
        }

        @Test
        void complete_sinJornadas_esRechazada() {
            assertThatThrownBy(() -> assignment.complete(5, null, TODAY))
                    .extracting("code").isEqualTo("asignacion.finalizar.sin.jornadas");
        }

        @Test
        void complete_conJornadaSinConfirmar_esRechazada() {
            assignment.registerWorkDay(MON, fullDay(), TODAY, TestData.NOW);

            assertThatThrownBy(() -> assignment.complete(5, null, TODAY))
                    .extracting("code").isEqualTo("asignacion.finalizar.jornadas.sin.confirmar");
        }

        @Test
        void complete_conSaldoPendiente_esRechazadaConElSaldoEnElMensaje() {
            confirmedDay(assignment, MON);

            assertThatThrownBy(() -> assignment.complete(5, null, TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(e -> assertThat(((BusinessRuleException) e).getArgs())
                            .containsExactly(new BigDecimal("80000")));
        }

        @Test
        void complete_aPazYSalvo_finalizaConLaFechaDelUltimoDiaTrabajado() {
            confirmedDay(assignment, MON);
            confirmedDay(assignment, TUE);
            new Settlement(assignment, "JOR-000002", new BigDecimal("160000"), PaymentMethod.CASH, null,
                    TUE.atTime(17, 0), MON, TUE, true);

            assignment.complete(4, "Cumplido", TODAY);

            assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
            assertThat(assignment.getActualEndDate()).isEqualTo(TUE);
            assertThat(assignment.getWorkerRating()).isEqualTo(4);
        }

        @Test
        void rateForeman_soloUnaVezYConValorEnRango() {
            confirmedDay(assignment, MON);
            new Settlement(assignment, "JOR-000003", new BigDecimal("80000"), PaymentMethod.CASH, null,
                    LocalDateTime.of(2026, 10, 19, 17, 0), MON, MON, true);
            assignment.complete(5, null, TODAY);

            assertThatThrownBy(() -> assignment.rateForeman(6)).extracting("code").isEqualTo("calificacion.fuera.de.rango");
            assignment.rateForeman(5);
            assertThatThrownBy(() -> assignment.rateForeman(4)).extracting("code").isEqualTo("asignacion.calificar.repetida");
        }
    }
}
