package com.jornal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jornal.entity.Assignment;
import com.jornal.entity.Foreman;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.service.SiteService;
import com.jornal.support.IntegrationTest;
import com.jornal.support.MutableClock;
import com.jornal.support.Scenario;
import com.jornal.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OWASP A01 (control de acceso roto) y A03 (inyeccion): un usuario no ve ni
 * modifica recursos de otro, y el texto digitado nunca se ejecuta como HTML.
 */
@IntegrationTest
class IdorAndXssIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Scenario scenario;
    @Autowired
    private SiteService siteService;
    @Autowired
    private MutableClock clock;

    private Foreman owner;
    private Foreman intruder;
    private Worker worker;
    private Worker otherWorker;
    private WorkFront front;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        clock.setTo(TestData.NOW);
        owner = scenario.foreman();
        intruder = scenario.foreman();
        worker = scenario.worker();
        otherWorker = scenario.worker();
        front = scenario.frontOf(owner);
        assignment = scenario.assignment(front, worker);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obraAjena_respondeNoEncontradaEnLugarDeProhibida() throws Exception {
        mvc.perform(get("/maestro/obras/{id}", front.getSite().getId()).with(user(scenario.login(intruder))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/maestro/obras/{id}", front.getSite().getId()).with(user(scenario.login(owner))))
                .andExpect(status().isOk());
    }

    @Test
    void asignacionDeOtroTrabajador_respondeNoEncontrada() throws Exception {
        mvc.perform(get("/trabajador/asignaciones/{id}", assignment.getId()).with(user(scenario.login(otherWorker))))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmarJornadaAjena_noCambiaNada() throws Exception {
        var day = assignment.registerWorkDay(TestData.TODAY.minusDays(1), TestData.fullDay(), TestData.TODAY,
                TestData.NOW);

        mvc.perform(post("/trabajador/jornadas/{id}/confirmar", day.getId())
                        .with(user(scenario.login(otherWorker))).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(day.getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    void idMalFormadoEnLaUrl_respondeNoEncontrado() throws Exception {
        mvc.perform(get("/maestro/obras/no-es-un-uuid").with(user(scenario.login(owner))))
                .andExpect(status().isNotFound());
    }

    /** Defensa en profundidad: aunque un controlador pasara otro id, @PreAuthorize lo detiene. */
    @Test
    void servicio_conIdDeOtroUsuario_niegaElAcceso() {
        var principal = scenario.login(intruder);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        assertThatThrownBy(() -> siteService.listSites(owner.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(siteService.listSites(intruder.getId())).isEmpty();
    }

    @Test
    void textoConScript_seMuestraEscapadoYNuncaSeEjecuta() throws Exception {
        String attack = "<script>alert('xss')</script>";
        MvcResult created = mvc.perform(post("/maestro/obras").with(user(scenario.login(owner))).with(csrf())
                        .param("name", attack).param("address", "Calle 1 # 2-3").param("neighborhood", "Centro")
                        .param("city", "C\u00facuta").param("startDate", "2026-11-02")
                        .param("estimatedEndDate", "2027-01-29"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mvc.perform(get(created.getResponse().getRedirectedUrl()).with(user(scenario.login(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString(attack))));
    }
}
