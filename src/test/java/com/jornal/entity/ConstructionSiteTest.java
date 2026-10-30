package com.jornal.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.support.TestData;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Maquina de estados de la obra: PLANNED -> IN_PROGRESS <-> SUSPENDED; IN_PROGRESS -> FINISHED. */
class ConstructionSiteTest {

    static Stream<Arguments> invalidTransitions() {
        Consumer<ConstructionSite> start = ConstructionSite::start;
        Consumer<ConstructionSite> suspend = ConstructionSite::suspend;
        Consumer<ConstructionSite> resume = ConstructionSite::resume;
        Consumer<ConstructionSite> finish = ConstructionSite::finish;
        return Stream.of(
                Arguments.of("planeada", (Consumer<ConstructionSite>) s -> { }, suspend, "obra.suspender.estado.invalido"),
                Arguments.of("planeada", (Consumer<ConstructionSite>) s -> { }, resume, "obra.reanudar.estado.invalido"),
                Arguments.of("planeada", (Consumer<ConstructionSite>) s -> { }, finish, "obra.finalizar.estado.invalido"),
                Arguments.of("en curso", start, start, "obra.iniciar.estado.invalido"),
                Arguments.of("en curso", start, resume, "obra.reanudar.estado.invalido"),
                Arguments.of("suspendida", start.andThen(suspend), finish, "obra.finalizar.estado.invalido"),
                Arguments.of("finalizada", start.andThen(finish), start, "obra.iniciar.estado.invalido"));
    }

    @ParameterizedTest(name = "[{index}] obra {0}: {3}")
    @MethodSource("invalidTransitions")
    void transicion_noPermitida_lanzaElCodigo(String state, Consumer<ConstructionSite> setUp,
                                              Consumer<ConstructionSite> action, String code) {
        ConstructionSite site = TestData.sitePlanned(TestData.foreman());
        setUp.accept(site);

        assertThatThrownBy(() -> action.accept(site)).extracting("code").isEqualTo(code);
    }

    @Test
    void cicloCompleto_planeadaEnCursoSuspendidaReanudadaFinalizada() {
        ConstructionSite site = TestData.sitePlanned(TestData.foreman());

        site.start();
        site.suspend();
        site.resume();
        site.finish();

        assertThat(site.getStatus()).isEqualTo(SiteStatus.FINISHED);
    }

    @Test
    void finish_conAsignacionesActivas_esRechazada() {
        ConstructionSite site = TestData.siteInProgress(TestData.foreman());
        WorkFront front = TestData.front(site);
        Assignment.invite(front, TestData.worker(), TestData.WAGE, TestData.TODAY, TestData.NOW);

        assertThatThrownBy(site::finish).extracting("code").isEqualTo("obra.finalizar.asignaciones.activas");
    }

    @Test
    void update_fechasQueDejanUnFrenteAfuera_esRechazada() {
        ConstructionSite site = TestData.siteInProgress(TestData.foreman());
        TestData.front(site);
        ConstructionSite.SiteData shorter = new ConstructionSite.SiteData("Casa Duarte", null, "Calle 12",
                "Caobos", "Cucuta", null, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 30));

        assertThatThrownBy(() -> site.update(shorter)).extracting("code").isEqualTo("obra.fechas.excluyen.frentes");
    }

    @Test
    void isDelayed_soloSiEstaEnCursoYPasoLaFechaEstimada() {
        ConstructionSite site = TestData.siteInProgress(TestData.foreman());

        assertThat(site.isDelayed(LocalDate.of(2026, 12, 31))).isFalse();
        assertThat(site.isDelayed(LocalDate.of(2027, 1, 1))).isTrue();
        site.suspend();
        assertThat(site.isDelayed(LocalDate.of(2027, 1, 1))).isFalse();
    }

    @Test
    void calculateLaborCost_sumaLoDevengadoDeTodosLosFrentes() {
        Assignment assignment = TestData.assignmentInProgress();
        TestData.confirmedDay(assignment, LocalDate.of(2026, 10, 26));
        assignment.registerWorkDay(LocalDate.of(2026, 10, 27), TestData.fullDay(), TestData.TODAY, TestData.NOW);

        assertThat(assignment.getFront().getSite().calculateLaborCost()).isEqualByComparingTo("160000");
    }
}
