package com.jornal.entity;

import static com.jornal.support.TestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.jornal.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkerTest {

    private WorkFront front;

    @BeforeEach
    void setUp() {
        front = TestData.front(TestData.siteInProgress(TestData.foreman()));
    }

    @Test
    void eligibilityIssues_trabajadorApto_noTieneMotivos() {
        assertThat(TestData.worker().eligibilityIssues(front, TODAY)).isEmpty();
    }

    @Test
    void eligibilityIssues_arlVenceAntesDelFinDelFrente_noEsApto() {
        Worker worker = TestData.worker(Trade.HELPER, front.getEndDate().minusDays(1));

        assertThat(worker.eligibilityIssues(front, TODAY)).containsExactly("elegibilidad.arl.vence.antes");
    }

    @Test
    void eligibilityIssues_arlVenceElMismoDiaQueElFrente_esApto() {
        Worker worker = TestData.worker(Trade.HELPER, front.getEndDate());

        assertThat(worker.eligibilityIssues(front, TODAY)).isEmpty();
    }

    @Test
    void eligibilityIssues_variosProblemas_losReportaTodos() {
        Worker worker = TestData.worker(Trade.WELDER, TODAY);
        worker.changeAvailability(false);

        assertThat(worker.eligibilityIssues(front, TODAY)).containsExactlyInAnyOrder(
                "elegibilidad.no.recibe.ofertas", "elegibilidad.oficio.distinto", "elegibilidad.arl.vence.antes");
    }

    @Test
    void eligibilityIssues_menorDeEdad_noEsApto() {
        Worker minor = TestData.workerBornOn(LocalDate.of(2010, 5, 1));

        assertThat(minor.eligibilityIssues(front, TODAY)).contains("elegibilidad.menor.de.edad");
    }
}
