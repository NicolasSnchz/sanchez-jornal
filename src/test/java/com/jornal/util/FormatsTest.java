package com.jornal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FormatsTest {

    @Test
    void money_usaPuntoDeMilesYRedondeaAlPeso() {
        assertThat(Formats.money(new BigDecimal("1234567.5"))).isEqualTo("$1.234.568");
        assertThat(Formats.money(BigDecimal.ZERO)).isEqualTo("$0");
    }

    @Test
    void date_diaMesAno() {
        assertThat(Formats.date(LocalDate.of(2026, 10, 9))).isEqualTo("09/10/2026");
    }

    @Test
    void timeScale_convierteHorasEnPorcentajeDeLaJornadaDiurna() {
        assertThat(TimeScale.position(LocalTime.of(6, 0))).isEqualTo("0.00");
        assertThat(TimeScale.position(LocalTime.of(12, 30))).isEqualTo("50.00");
        assertThat(TimeScale.position(LocalTime.of(19, 0))).isEqualTo("100.00");
        assertThat(TimeScale.length(120)).isEqualTo("15.38");
        assertThat(TimeScale.length(2000)).isEqualTo("100.00");
    }
}
