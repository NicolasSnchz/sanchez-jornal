package com.jornal.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jornal.entity.Foreman;
import com.jornal.entity.Worker;
import com.jornal.support.IntegrationTest;
import com.jornal.support.MutableClock;
import com.jornal.support.Scenario;
import com.jornal.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Seguridad de la parte web: sesion, roles, CSRF, bloqueo y encabezados. */
@IntegrationTest
class WebSecurityIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Scenario scenario;
    @Autowired
    private MutableClock clock;

    private Foreman foreman;
    private Worker worker;

    @BeforeEach
    void setUp() {
        clock.setTo(TestData.NOW);
        foreman = scenario.foreman();
        worker = scenario.worker();
    }

    @Test
    void paginaProtegida_sinSesion_redirigeAlLogin() throws Exception {
        mvc.perform(get("/maestro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void login_respondeConEncabezadosDeSeguridad() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "same-origin"));
    }

    @Test
    void formLogin_maestro_vaASuPanel() throws Exception {
        mvc.perform(formLogin("/login").user("email", foreman.getEmail()).password(Scenario.PASSWORD))
                .andExpect(redirectedUrl("/maestro"));
    }

    @Test
    void formLogin_trabajador_vaASuPanel() throws Exception {
        mvc.perform(formLogin("/login").user("email", worker.getEmail()).password(Scenario.PASSWORD))
                .andExpect(redirectedUrl("/trabajador"));
    }

    @Test
    void formLogin_claveIncorrecta_noRevelaSiElCorreoExiste() throws Exception {
        mvc.perform(formLogin("/login").user("email", foreman.getEmail()).password("otra-clave-1"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin("/login").user("email", "nadie@prueba.co").password("otra-clave-1"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void formLogin_cincoIntentosFallidos_bloqueaLaCuentaAunConLaClaveCorrecta() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(formLogin("/login").user("email", foreman.getEmail()).password("mala-clave-" + i));
        }

        mvc.perform(formLogin("/login").user("email", foreman.getEmail()).password(Scenario.PASSWORD))
                .andExpect(redirectedUrl("/login?bloqueada"));
    }

    @Test
    void trabajador_noEntraAPaginasDelMaestro() throws Exception {
        mvc.perform(get("/maestro/obras").with(user(scenario.login(worker))))
                .andExpect(status().isForbidden());
    }

    @Test
    void maestro_noEntraAPaginasDelTrabajador() throws Exception {
        mvc.perform(get("/trabajador").with(user(scenario.login(foreman))))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_sinTokenCsrf_esRechazado() throws Exception {
        mvc.perform(post("/maestro/obras").with(user(scenario.login(foreman)))
                        .param("name", "Obra").param("address", "Calle 1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_conTokenCsrf_esAceptado() throws Exception {
        mvc.perform(post("/maestro/obras").with(user(scenario.login(foreman))).with(csrf())
                        .param("name", "Obra nueva").param("address", "Calle 1 # 2-3")
                        .param("neighborhood", "Centro").param("city", "C\u00facuta")
                        .param("startDate", "2026-11-02").param("estimatedEndDate", "2027-01-29"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/maestro/obras/*"));
    }

    @Test
    void logout_cierraLaSesion() throws Exception {
        mvc.perform(logout()).andExpect(redirectedUrl("/login?salida"));
    }

    @Test
    void actuatorHealth_esPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void actuatorEnv_noEstaExpuesto() throws Exception {
        mvc.perform(get("/actuator/env").with(user(scenario.login(foreman))))
                .andExpect(status().isNotFound());
    }
}
