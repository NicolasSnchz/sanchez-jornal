package com.jornal.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jornal.config.ClockConfig;
import com.jornal.config.PropertiesConfig;
import com.jornal.config.SecurityConfig;
import com.jornal.controller.api.ForemanApiController;
import com.jornal.dto.form.SiteForm;
import com.jornal.dto.view.SiteSummary;
import com.jornal.entity.SiteStatus;
import com.jornal.exception.ApiExceptionHandler;
import com.jornal.exception.BusinessRuleException;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.security.ApiErrorWriter;
import com.jornal.security.JornalUserDetails;
import com.jornal.security.JornalUserDetailsService;
import com.jornal.security.JwtService;
import com.jornal.service.AssignmentService;
import com.jornal.service.PaymentService;
import com.jornal.service.SiteService;
import com.jornal.service.WorkDayService;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de la capa web de la API aislada (@WebMvcTest): la seguridad y el
 * manejo de errores son reales; los servicios y el JWT se simulan con
 * {@code @MockitoBean}. Verifica que cada excepcion se traduzca al codigo HTTP
 * correcto con el cuerpo ApiError, sin filtrar detalles internos.
 */
@WebMvcTest(ForemanApiController.class)
@Import({SecurityConfig.class, PropertiesConfig.class, ClockConfig.class, ApiErrorWriter.class,
        ApiExceptionHandler.class})
@ActiveProfiles("test")
class ForemanApiControllerTest {

    private static final String FOREMAN_TOKEN = "token-de-maestro";
    private static final String WORKER_TOKEN = "token-de-trabajador";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SiteService siteService;
    @MockitoBean
    private AssignmentService assignmentService;
    @MockitoBean
    private WorkDayService workDayService;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JornalUserDetailsService userDetailsService;

    private JornalUserDetails foreman;

    @BeforeEach
    void tokens() {
        foreman = JornalUserDetails.from(TestData.foreman(), TestData.NOW);
        JornalUserDetails worker = JornalUserDetails.from(TestData.worker(), TestData.NOW);
        when(jwtService.validateAndGetUserId(FOREMAN_TOKEN)).thenReturn(foreman.id());
        when(jwtService.validateAndGetUserId(WORKER_TOKEN)).thenReturn(worker.id());
        when(userDetailsService.loadById(foreman.id())).thenReturn(foreman);
        when(userDetailsService.loadById(worker.id())).thenReturn(worker);
    }

    @Test
    void listarObras_conToken_200ConLasObrasDelMaestroDelToken() throws Exception {
        SiteSummary site = new SiteSummary(UUID.randomUUID(), "Casa Duarte", "Calle 12 # 2E-45", "Caobos",
                "C\u00facuta", SiteStatus.IN_PROGRESS, false, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 12, 11),
                3, 4L, new BigDecimal("6016251"));
        when(siteService.listSites(foreman.id())).thenReturn(List.of(site));

        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Casa Duarte"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].laborCost").value(6016251));
    }

    @Test
    void crearObra_valida_201YUsaElIdDelToken() throws Exception {
        UUID siteId = UUID.randomUUID();
        when(siteService.createSite(eq(foreman.id()), any(SiteForm.class))).thenReturn(siteId);

        mvc.perform(post("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validSite()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/obras/" + siteId))
                .andExpect(jsonPath("$.id").value(siteId.toString()));
        verify(siteService).createSite(eq(foreman.id()), any(SiteForm.class));
    }

    @Test
    void crearObra_invalida_400SinLlegarAlServicio() throws Exception {
        mvc.perform(post("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_VALIDACION"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("address")));
        verifyNoInteractions(siteService);
    }

    @Test
    void jsonMalFormado_400() throws Exception {
        mvc.perform(post("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_PETICION_INVALIDA"));
    }

    @Test
    void reglaDeNegocio_422ConMensajeEnEspanolYDineroFormateado() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        when(paymentService.register(eq(foreman.id()), eq(assignmentId), any()))
                .thenThrow(new BusinessRuleException("pago.anticipo.excede.saldo", new BigDecimal("240000")));

        mvc.perform(post("/api/asignaciones/{id}/pagos", assignmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ADVANCE\",\"amount\":300000,\"method\":\"CASH\",\"reason\":\"Botas\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAGO_ANTICIPO_EXCEDE_SALDO"))
                .andExpect(jsonPath("$.message").value(containsString("$240.000")));
    }

    @Test
    void recursoAjeno_404() throws Exception {
        UUID siteId = UUID.randomUUID();
        when(siteService.getSite(foreman.id(), siteId)).thenThrow(new ResourceNotFoundException("obra.no.encontrada"));

        mvc.perform(get("/api/obras/{id}", siteId).header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OBRA_NO_ENCONTRADA"));
    }

    @Test
    void cambioConcurrente_409() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        when(paymentService.register(eq(foreman.id()), eq(assignmentId), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Assignment", assignmentId));

        mvc.perform(post("/api/asignaciones/{id}/pagos", assignmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"FINAL_SETTLEMENT\",\"method\":\"CASH\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO_CONCURRENTE"));
    }

    @Test
    void errorInesperado_500SinDetallesInternos() throws Exception {
        when(siteService.listSites(foreman.id())).thenThrow(new IllegalStateException("tabla users corrupta"));

        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREMAN_TOKEN))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INESPERADO"))
                .andExpect(content().string(not(containsString("users"))));
    }

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(get("/api/obras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("API_AUTENTICACION_REQUERIDA"));
    }

    @Test
    void tokenDeTrabajador_403() throws Exception {
        mvc.perform(get("/api/obras").header(HttpHeaders.AUTHORIZATION, "Bearer " + WORKER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("API_ACCESO_DENEGADO"));
        verifyNoInteractions(siteService);
    }

    private static String validSite() {
        return """
                {"name":"Casa Duarte","address":"Calle 12 # 2E-45","neighborhood":"Caobos","city":"Cucuta",
                 "startDate":"2026-11-02","estimatedEndDate":"2027-01-29"}
                """;
    }
}
