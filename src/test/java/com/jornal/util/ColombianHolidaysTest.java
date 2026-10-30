package com.jornal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ColombianHolidaysTest {

    @ParameterizedTest(name = "Pascua {0} = {1}")
    @CsvSource({"2025, 2025-04-20", "2026, 2026-04-05", "2027, 2027-03-28", "2028, 2028-04-16"})
    void easterSunday_algoritmoGregoriano(int year, LocalDate expected) {
        assertThat(ColombianHolidays.easterSunday(year)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} es {1}")
    @CsvSource(delimiter = '|', value = {
        "2026-01-01|A\u00f1o Nuevo",
        "2026-01-12|Reyes Magos",                 // 6 de enero cae martes: se pasa al lunes
        "2026-03-23|San Jos\u00e9",
        "2026-04-02|Jueves Santo",
        "2026-04-03|Viernes Santo",
        "2026-05-18|Ascensi\u00f3n del Se\u00f1or",
        "2026-06-08|Corpus Christi",
        "2026-06-15|Sagrado Coraz\u00f3n",
        "2026-08-17|Asunci\u00f3n de la Virgen",
        "2026-10-12|D\u00eda de la Raza",
        "2026-11-02|Todos los Santos",            // 1 de noviembre cae domingo
        "2026-11-16|Independencia de Cartagena",
        "2026-12-25|Navidad"
    })
    void nameOf_festivosDe2026(LocalDate date, String name) {
        assertThat(ColombianHolidays.nameOf(date)).contains(name);
    }

    @Test
    void holidaysOf_colombiaTieneDieciochoFestivosAlAno() {
        assertThat(ColombianHolidays.holidaysOf(2026)).hasSize(18);
        assertThat(ColombianHolidays.holidaysOf(2027)).hasSize(18);
    }

    @Test
    void isHoliday_diaComun_noEsFestivo() {
        assertThat(ColombianHolidays.isHoliday(LocalDate.of(2026, 10, 29))).isFalse();
        assertThat(ColombianHolidays.isHoliday(LocalDate.of(2026, 11, 1))).isFalse();
    }
}
