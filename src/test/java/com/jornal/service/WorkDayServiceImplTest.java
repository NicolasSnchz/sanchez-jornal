package com.jornal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jornal.dto.form.WorkDayForm;
import com.jornal.entity.Assignment;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkDayStatus;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.WorkDayRepository;
import com.jornal.service.impl.WorkDayServiceImpl;
import com.jornal.support.Clocks;
import com.jornal.support.TestData;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkDayServiceImplTest {

    private static final UUID FOREMAN_ID = UUID.randomUUID();

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private WorkDayRepository workDayRepository;

    private WorkDayServiceImpl service;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        service = new WorkDayServiceImpl(assignmentRepository, workDayRepository,
                new ViewMapper(Clocks.fixedAtTestNow()), Clocks.fixedAtTestNow());
        assignment = TestData.assignmentInProgress();
    }

    @Test
    void prepareWorkDay_sinFecha_sugiereHoyConElHorarioDelFrente() {
        when(assignmentRepository.findByIdAndFrontSiteForemanId(assignment.getId(), FOREMAN_ID))
                .thenReturn(Optional.of(assignment));

        WorkDayForm form = service.prepareWorkDay(FOREMAN_ID, assignment.getId(), null);

        assertThat(form.getDate()).isEqualTo(TestData.TODAY);
        assertThat(form.getCheckInTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(form.getCheckOutTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(form.getBreakMinutes()).isEqualTo(60);
    }

    @Test
    void prepareWorkDay_hoyYaRegistrado_sugiereElDiaLaboralAnteriorSinRegistro() {
        assignment.registerWorkDay(TestData.TODAY, TestData.fullDay(), TestData.TODAY, TestData.NOW);
        assignment.registerWorkDay(TestData.TODAY.minusDays(1), TestData.fullDay(), TestData.TODAY, TestData.NOW);
        when(assignmentRepository.findByIdAndFrontSiteForemanId(assignment.getId(), FOREMAN_ID))
                .thenReturn(Optional.of(assignment));

        WorkDayForm form = service.prepareWorkDay(FOREMAN_ID, assignment.getId(), null);

        assertThat(form.getDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    void correctWorkDay_cambiandoLaFecha_esRechazado() {
        WorkDay day = assignment.registerWorkDay(LocalDate.of(2026, 10, 27), TestData.fullDay(), TestData.TODAY,
                TestData.NOW);
        when(workDayRepository.findByIdAndAssignmentFrontSiteForemanId(day.getId(), FOREMAN_ID))
                .thenReturn(Optional.of(day));
        WorkDayForm form = WorkDayForm.from(day);
        form.setDate(LocalDate.of(2026, 10, 28));

        assertThatThrownBy(() -> service.correctWorkDay(FOREMAN_ID, day.getId(), form))
                .extracting("code").isEqualTo("jornada.corregir.fecha");
    }

    @Test
    void confirmAllPending_confirmaLasPendientesYNoTocaLasObjetadas() {
        UUID workerId = assignment.getWorker().getId();
        assignment.registerWorkDay(LocalDate.of(2026, 10, 26), TestData.fullDay(), TestData.TODAY, TestData.NOW);
        assignment.registerWorkDay(LocalDate.of(2026, 10, 27), TestData.fullDay(), TestData.TODAY, TestData.NOW);
        WorkDay disputed = assignment.registerWorkDay(LocalDate.of(2026, 10, 28), TestData.fullDay(), TestData.TODAY,
                TestData.NOW);
        disputed.dispute("Sali mas tarde", TestData.NOW);
        when(assignmentRepository.findByIdAndWorkerId(assignment.getId(), workerId)).thenReturn(Optional.of(assignment));

        int confirmed = service.confirmAllPending(workerId, assignment.getId());

        assertThat(confirmed).isEqualTo(2);
        assertThat(disputed.getStatus()).isEqualTo(WorkDayStatus.DISPUTED);
    }
}
