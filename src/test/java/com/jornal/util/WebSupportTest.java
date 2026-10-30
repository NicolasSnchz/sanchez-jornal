package com.jornal.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.jornal.config.LaborProperties;
import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.SafeRedirect;
import com.jornal.controller.support.ViewFormats;
import com.jornal.dto.api.ApiError;
import com.jornal.service.WagePolicy;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

/** Piezas pequenas pero importantes: politica de jornal, redireccion segura y formatos. */
class WebSupportTest {

    private final WagePolicy wagePolicy = new WagePolicy(new LaborProperties(new BigDecimal("1750905"), 210));

    @ParameterizedTest(name = "{0} min diarios -> jornal minimo {1}")
    @CsvSource({"480, 66702", "420, 58364", "360, 50026"})
    void minimumDailyWage_proporcionalAlHorario(int minutes, long expected) {
        assertThat(wagePolicy.minimumDailyWage(minutes)).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @ParameterizedTest(name = "Referer {0} -> {1}")
    @CsvSource({
        "http://localhost/maestro/obras?x=1, /maestro/obras?x=1",
        "https://evil.example/robar, /",
        "//evil.example/robar, /",
        "javascript:alert(1), /"
    })
    void safeRedirect_soloVuelveARutasDelMismoSitio(String referer, String expected) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.addHeader("Referer", referer);

        assertThat(SafeRedirect.back(request, "/")).isEqualTo(expected);
    }

    @Test
    void messageArgs_formateaDineroYFechas() {
        Object[] args = MessageArgs.format(new BigDecimal("66702"), LocalDate.of(2026, 10, 16), 3);

        assertThat(args).containsExactly("$66.702", "16/10/2026", 3);
    }

    @Test
    void viewFormats_diasSeguidosYDuraciones() {
        ViewFormats fmt = new ViewFormats();

        assertThat(fmt.days(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))).isEqualTo("lun a vie");
        assertThat(fmt.days(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))).isEqualTo("lun, mi\u00e9");
        assertThat(fmt.duration(510)).isEqualTo("8 h 30 min");
        assertThat(fmt.duration(45)).isEqualTo("45 min");
        assertThat(fmt.percent(new BigDecimal("0.50"))).isEqualTo("50 %");
        assertThat(fmt.weekdayDate(LocalDate.of(2026, 10, 29))).isEqualTo("jue 29 oct");
        assertThat(fmt.monthName(YearMonth.of(2026, 10))).isEqualTo("octubre");
    }

    @Test
    void apiError_codigoDeMensajeAMayusculas() {
        assertThat(ApiError.toApiCode("jornada.periodo.liquidado")).isEqualTo("JORNADA_PERIODO_LIQUIDADO");
    }
}
