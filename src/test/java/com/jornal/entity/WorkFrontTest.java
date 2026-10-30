package com.jornal.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WorkFrontTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2026, 12, 18);
    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    private static final Set<DayOfWeek> MON_TO_SAT = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);

    private ConstructionSite site;

    @BeforeEach
    void setUp() {
        site = TestData.siteInProgress(TestData.foreman());
    }

    @Test
    void openFront_horarioTipico_calculaHorasYValorHora() {
        WorkFront front = site.openFront(TestData.frontData(2));

        assertThat(front.scheduledMinutesPerDay()).isEqualTo(480);
        assertThat(front.scheduledMinutesPerWeek()).isEqualTo(2400);
        assertThat(front.hourlyRate(new BigDecimal("80000"))).isEqualByComparingTo("10000");
        assertThat(front.availableOpenings()).isEqualTo(2);
    }

    @Test
    void openFront_cuarentaYDosHorasExactas_esValido() {
        WorkFront front = site.openFront(data(3, "80000", MON_TO_SAT, "07:00", "15:00", 60));

        assertThat(front.scheduledMinutesPerWeek()).isEqualTo(42 * 60);
    }

    static Stream<Arguments> invalidFronts() {
        return Stream.of(
                Arguments.of(new WorkFront.FrontData("Labor", Trade.HELPER, 1, new BigDecimal("80000"), END, START,
                        WEEKDAYS, LocalTime.of(7, 0), LocalTime.of(16, 0), 60), "frente.fechas.invalidas"),
                Arguments.of(data(0, "80000", WEEKDAYS, "07:00", "16:00", 60), "frente.cupos.invalidos"),
                Arguments.of(data(51, "80000", WEEKDAYS, "07:00", "16:00", 60), "frente.cupos.invalidos"),
                Arguments.of(data(1, "0", WEEKDAYS, "07:00", "16:00", 60), "frente.jornal.invalido"),
                Arguments.of(data(1, "80000", EnumSet.noneOf(DayOfWeek.class), "07:00", "16:00", 60), "frente.dias.vacios"),
                Arguments.of(data(1, "80000", WEEKDAYS, "16:00", "07:00", 60), "frente.horario.invalido"),
                Arguments.of(data(1, "80000", WEEKDAYS, "05:00", "14:00", 60), "frente.horario.no.diurno"),
                Arguments.of(data(1, "80000", WEEKDAYS, "10:00", "19:30", 60), "frente.horario.no.diurno"),
                Arguments.of(data(1, "80000", WEEKDAYS, "07:00", "16:00", 130), "frente.almuerzo.invalido"),
                Arguments.of(data(1, "80000", WEEKDAYS, "07:00", "08:00", 60), "frente.almuerzo.invalido"),
                // 7,5 h x 6 dias = 45 h: supera las 42 h de la Ley 2101
                Arguments.of(data(1, "80000", MON_TO_SAT, "07:00", "15:30", 60), "frente.horas.semanales.exceden"));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidFronts")
    void openFront_datosInvalidos_lanzaElCodigoDeLaRegla(WorkFront.FrontData data, String code) {
        assertThatThrownBy(() -> site.openFront(data)).extracting("code").isEqualTo(code);
        assertThat(site.getFronts()).isEmpty();
    }

    @Test
    void openFront_fechasFueraDeLaObra_esRechazado() {
        WorkFront.FrontData data = new WorkFront.FrontData("Labor", Trade.HELPER, 1, new BigDecimal("80000"),
                LocalDate.of(2026, 9, 1), END, WEEKDAYS, LocalTime.of(7, 0), LocalTime.of(16, 0), 60);

        assertThatThrownBy(() -> site.openFront(data)).extracting("code").isEqualTo("frente.fechas.fuera.de.obra");
    }

    @Test
    void isWorkingDay_soloLosDiasConfiguradosQueNoSonFestivos() {
        WorkFront front = site.openFront(TestData.frontData(1));

        assertThat(front.isWorkingDay(LocalDate.of(2026, 10, 30))).isTrue();   // viernes
        assertThat(front.isWorkingDay(LocalDate.of(2026, 10, 31))).isFalse();  // sabado
        assertThat(front.isWorkingDay(LocalDate.of(2026, 11, 2))).isFalse();   // lunes festivo
        assertThat(front.isScheduledDay(LocalDate.of(2026, 11, 2))).isTrue();
    }

    @Test
    void openFront_conDomingo_esRechazadoPorqueLlevaRecargoDominical() {
        Set<DayOfWeek> withSunday = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        assertThatThrownBy(() -> site.openFront(data(1, "80000", withSunday, "07:00", "15:00", 60)))
                .extracting("code").isEqualTo("frente.domingo.no.permitido");
    }

    @Test
    void availableOpenings_invitacionesYAsignacionesActivasOcupanCupo() {
        WorkFront front = site.openFront(TestData.frontData(2));
        Assignment first = Assignment.invite(front, TestData.worker(), TestData.WAGE, START, START.minusDays(1).atStartOfDay());
        Assignment.invite(front, TestData.worker(), TestData.WAGE, START, START.minusDays(1).atStartOfDay());

        assertThat(front.availableOpenings()).isZero();

        first.reject(START.atStartOfDay());

        assertThat(front.availableOpenings()).isEqualTo(1);
    }

    private static WorkFront.FrontData data(int openings, String wage, Set<DayOfWeek> days, String start, String end,
                                            int lunch) {
        return new WorkFront.FrontData("Labor", Trade.HELPER, openings, new BigDecimal(wage), START, END, days,
                LocalTime.parse(start), LocalTime.parse(end), lunch);
    }
}
