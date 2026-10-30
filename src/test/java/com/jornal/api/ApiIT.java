package com.jornal.api;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jornal.entity.Assignment;
import com.jornal.entity.Foreman;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.support.IntegrationTest;
import com.jornal.support.MutableClock;
import com.jornal.support.Scenario;
import com.jornal.support.TestData;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API REST con autenticacion JWT real: login, token, roles, formato uniforme de
 * errores (ApiError) y los codigos HTTP de cada caso.
 */
@IntegrationTest
class ApiIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Scenario scenario;
    @Autowired
    private MutableClock clock;
    @Autowired
    private ObjectMapper json;

    private Foreman foreman;
    private Worker worker;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        clock.setTo(TestData.NOW);
        foreman = scenario.foreman();
        worker = scenario.worker();
        WorkFront front = scenario.frontOf(foreman);
        assignment = scenario.assignment(front, worker);
    }

    @Test
    void login_credencialesValidas_entregaUnTokenBearer() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(foreman.getEmail(), Scenario.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.role").value("FOREMAN"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_claveIncorrecta_401ConErrorUniforme() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(foreman.getEmail(), "clave-incorrecta-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("API_CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void recursoProtegido_sinToken_401() throws Exception {
        mvc.perform(get("/api/obras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("API_AUTENTICACION_REQUERIDA"));
    }

    @Test
    void recursoProtegido_tokenAlterado_401() throws Exception {
        String token = tokenOf(foreman.getEmail());
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("API_TOKEN_INVALIDO"));
    }

    @Test
    void recursoProtegido_tokenExpirado_401() throws Exception {
        String token = tokenOf(foreman.getEmail());
        clock.advance(Duration.ofHours(2));

        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("API_TOKEN_INVALIDO"));
    }

    /**
     * Las dos cadenas de filtros conviven sin mezclarse: la sesion del navegador no
     * abre la API y el token JWT no abre las paginas web.
     */
    @Test
    void sesionWebYTokenJwt_alMismoTiempo_cadaUnoSoloAbreSuPropiaCadena() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(formLogin("/login").user("email", foreman.getEmail())
                        .password("password", Scenario.PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);
        String bearer = bearer(foreman.getEmail());

        mvc.perform(get("/maestro").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/obras").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        mvc.perform(get("/maestro").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login")));
    }

    @Test
    void maestro_listaSusObras() throws Exception {
        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, bearer(foreman.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Casa Duarte"));
    }

    @Test
    void trabajador_enRecursoDeMaestro_403() throws Exception {
        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, bearer(worker.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("API_ACCESO_DENEGADO"));
    }

    @Test
    void crearObra_datosInvalidos_400ConLosCampos() throws Exception {
        mvc.perform(post("/api/obras").header(HttpHeaders.AUTHORIZATION, bearer(foreman.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_VALIDACION"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("startDate")));
    }

    @Test
    void crearObra_reglaDeNegocio_422ConSuCodigo() throws Exception {
        String body = """
                {"name":"Obra","address":"Calle 1","neighborhood":"Centro","city":"Cucuta",
                 "startDate":"2026-01-05","estimatedEndDate":"2026-03-31"}
                """;
        mvc.perform(post("/api/obras").header(HttpHeaders.AUTHORIZATION, bearer(foreman.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OBRA_FIN_PASADO"));
    }

    @Test
    void crearObra_valida_201ConLocation() throws Exception {
        String body = """
                {"name":"Local Avenida 0","address":"Av. 0 # 11-25","neighborhood":"Centro","city":"Cucuta",
                 "startDate":"2026-11-09","estimatedEndDate":"2027-01-29"}
                """;
        mvc.perform(post("/api/obras").header(HttpHeaders.AUTHORIZATION, bearer(foreman.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/obras/")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void trabajador_confirmaSuJornadaUnaSolaVez() throws Exception {
        var day = assignment.registerWorkDay(TestData.TODAY.minusDays(1), TestData.fullDay(), TestData.TODAY,
                TestData.NOW);

        mvc.perform(post("/api/jornadas/{id}/confirmacion", day.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(worker.getEmail())))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/jornadas/{id}/confirmacion", day.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(worker.getEmail())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("JORNADA_CONFIRMAR_ESTADO_INVALIDO"));
    }

    @Test
    void recursoDeOtroMaestro_404() throws Exception {
        Foreman other = scenario.foreman();

        mvc.perform(get("/api/asignaciones/{id}", assignment.getId()).header(HttpHeaders.AUTHORIZATION,
                        bearer(other.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASIGNACION_NO_ENCONTRADA"));
    }

    @Test
    void me_devuelveLaCuentaDelToken() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(worker.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("WORKER"))
                .andExpect(jsonPath("$.email").value(worker.getEmail()));
    }

    private String bearer(String email) throws Exception {
        return "Bearer " + tokenOf(email);
    }

    private String tokenOf(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, Scenario.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("accessToken").asText();
    }

    private String credentials(String email, String password) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }
}
