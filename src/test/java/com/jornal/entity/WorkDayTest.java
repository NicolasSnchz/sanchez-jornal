package com.jornal.entity;

import static com.jornal.support.TestData.TODAY;
import static com.jornal.support.TestData.WAGE;
import static com.jornal.support.TestData.record;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.exception.BusinessRuleException;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Calculo del valor de una jornada y sus reglas de horario. Horario programado: 480 min. */
class WorkDayTest {

    private static final int SCHEDULED = 480;
    private static final LocalDate DAY = LocalDate.of(2026, 10, 27);

    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignment = TestData.assignmentInProgress();
    }

    @DisplayName("value: jornal x (minutos ordinarios + extra x 1,25) / minutos programados, redondeado al peso")
    @ParameterizedTest(name = "[{index}] {0} a {1} con {2} min de descanso = {3}")
    @CsvSource({
        "07:00, 16:00, 60,  80000",   // jornada completa
        "07:00, 11:00,  0,  40000",   // media jornada (lluvia)
        "07:00, 12:00,  0,  50000",   // 5 de 8 horas
        "07:30, 16:00, 60,  75000",   // llego media hora tarde
        "07:00, 16:00, 90,  75000",   // almuerzo largo
        "07:00, 17:00, 60,  92500",   // 1 hora extra: 80000 x 1,15625
        "07:00, 18:00, 60, 105000",   // 2 horas extra: el maximo legal por dia
        "07:00, 15:47, 60,  77833"    // 467 min: 77833,33 se redondea a 77833
    })
    void value_segunHorasTrabajadas_calculaElValorEnPesos(String in, String out, int breakMinutes, long expected) {
        WorkDay day = assignment.registerWorkDay(DAY, record(in, out, breakMinutes), TODAY, TestData.NOW);

        assertThat(day.value(WAGE, SCHEDULED)).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @Test
    void overtimeMinutes_jornadaMasLarga_soloCuentaLoQueSuperaElHorario() {
        WorkDay day = assignment.registerWorkDay(DAY, record("07:00", "17:30", 60), TODAY, TestData.NOW);

        assertThat(day.workedMinutes()).isEqualTo(570);
        assertThat(day.overtimeMinutes(SCHEDULED)).isEqualTo(90);
        assertThat(day.fraction(SCHEDULED)).isEqualByComparingTo("1.00");
    }

    @Test
    void fraction_mediaJornada_esCeroPuntoCincuenta() {
        WorkDay day = assignment.registerWorkDay(DAY, record("07:00", "11:00", 0), TODAY, TestData.NOW);

        assertThat(day.fraction(SCHEDULED)).isEqualByComparingTo("0.50");
    }

    @DisplayName("validate: rechaza horarios que no cumplen la ley o no tienen sentido")
    @ParameterizedTest(name = "[{index}] {0} a {1}, descanso {2} -> {3}")
    @CsvSource({
        "05:30, 14:00,  60, jornada.horario.no.diurno",
        "07:00, 19:30,  60, jornada.horario.no.diurno",
        "10:00, 09:00,   0, jornada.horario.invalido",
        "07:00, 07:00,   0, jornada.horario.invalido",
        "07:00, 16:00,  -5, jornada.descanso.invalido",
        "07:00, 08:00,  60, jornada.descanso.invalido",
        "07:00, 16:00, 200, jornada.descanso.invalido",
        "06:00, 19:00,  60, jornada.horas.extra.exceden"
    })
    void validate_horarioInvalido_lanzaReglaDeNegocio(String in, String out, int breakMinutes, String code) {
        assertThatThrownBy(() -> WorkDay.validate(record(in, out, breakMinutes), SCHEDULED))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo(code);
    }

    @Test
    void validate_dosHorasExtraExactas_esValido() {
        WorkDay.validate(record("07:00", "18:00", 60), SCHEDULED);
    }

    @Test
    void getStatus_recienRegistrada_estaPorConfirmar() {
        WorkDay day = assignment.registerWorkDay(DAY, TestData.fullDay(), TODAY, TestData.NOW);

        assertThat(day.getStatus()).isEqualTo(WorkDayStatus.PENDING);
    }

    @Test
    void confirm_jornadaPendiente_quedaConfirmadaYNoSePuedeConfirmarOtraVez() {
        WorkDay day = assignment.registerWorkDay(DAY, TestData.fullDay(), TODAY, TestData.NOW);

        day.confirm(TestData.NOW);

        assertThat(day.getStatus()).isEqualTo(WorkDayStatus.CONFIRMED);
        assertThatThrownBy(() -> day.confirm(TestData.NOW))
                .extracting("code").isEqualTo("jornada.confirmar.estado.invalido");
    }

    @Test
    void dispute_sinMotivo_esRechazada() {
        WorkDay day = assignment.registerWorkDay(DAY, TestData.fullDay(), TODAY, TestData.NOW);

        assertThatThrownBy(() -> day.dispute("  ", TestData.NOW))
                .extracting("code").isEqualTo("jornada.objecion.motivo.requerido");
        assertThat(day.getStatus()).isEqualTo(WorkDayStatus.PENDING);
    }

    @Test
    void correct_jornadaObjetada_vuelveAPorConfirmarYConservaElMotivo() {
        WorkDay day = assignment.registerWorkDay(DAY, TestData.fullDay(), TODAY, TestData.NOW);
        day.dispute("Sali a las 5:00 p. m.", TestData.NOW);

        assignment.correctWorkDay(day, record("07:00", "17:00", 60), TestData.NOW);

        assertThat(day.getStatus()).isEqualTo(WorkDayStatus.PENDING);
        assertThat(day.wasCorrectedAfterDispute()).isTrue();
        assertThat(day.getDisputeReason()).isEqualTo("Sali a las 5:00 p. m.");
        assertThat(day.getCheckOutTime()).hasToString("17:00");
    }

    @Test
    void correct_jornadaConfirmada_esRechazada() {
        WorkDay day = TestData.confirmedDay(assignment, DAY);

        assertThatThrownBy(() -> assignment.correctWorkDay(day, record("07:00", "17:00", 60), TestData.NOW))
                .extracting("code").isEqualTo("jornada.corregir.confirmada");
    }
}
