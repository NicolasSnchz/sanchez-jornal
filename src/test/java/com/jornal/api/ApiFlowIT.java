package com.jornal.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jornal.entity.Foreman;
import com.jornal.entity.WorkFront;
import com.jornal.entity.Worker;
import com.jornal.support.IntegrationTest;
import com.jornal.support.MutableClock;
import com.jornal.support.Scenario;
import com.jornal.support.TestData;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * El mismo caso de uso de PayrollFlowIT, pero como lo haria una app movil: solo
 * peticiones HTTP a /api/** con el token JWT de cada usuario. Prueba que la API
 * expone todo el ciclo (invitar, aceptar, registrar, objetar, corregir, confirmar,
 * pagar y confirmar el pago) y que las cuentas cuadran al peso.
 */
@IntegrationTest
class ApiFlowIT {

    /** Semana sin festivos. */
    private static final LocalDate MON = LocalDate.of(2026, 11, 23);
    private static final LocalDate TUE = MON.plusDays(1);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Scenario scenario;
    @Autowired
    private MutableClock clock;
    @Autowired
    private ObjectMapper json;

    @Test
    void cicloCompleto_soloConLaApi_registraObjetaCorrigePagaYCuadraAlPeso() throws Exception {
        clock.setTo(TestData.NOW);
        Foreman foreman = scenario.foreman();
        Worker worker = scenario.worker();
        WorkFront front = scenario.frontOf(foreman);
        String maestro = bearer(foreman.getEmail());
        String trabajador = bearer(worker.getEmail());

        // 1. El maestro consulta su obra, el frente y encuentra al trabajador como candidato apto
        call(get("/api/obras/{id}", front.getSite().getId()), maestro).andExpect(status().isOk());
        call(get("/api/frentes/{id}", front.getId()), maestro).andExpect(status().isOk());
        call(get("/api/frentes/{id}/candidatos", front.getId()), maestro)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.workerId == '" + worker.getId() + "')].eligible").value(hasItem(true)));

        // 2. Lo invita y el trabajador acepta
        String assignmentId = idFrom(send(post("/api/frentes/{id}/invitaciones", front.getId()), maestro,
                Map.of("workerId", worker.getId(), "dailyWage", 80000, "startDate", MON.toString()))
                .andExpect(status().isCreated()));
        call(get("/api/mis-asignaciones"), trabajador)
                .andExpect(jsonPath("$[*].id").value(hasItem(assignmentId)));
        send(post("/api/mis-asignaciones/{id}/respuesta", assignmentId), trabajador, Map.of("accept", true))
                .andExpect(status().isNoContent());
        call(get("/api/asignaciones"), maestro).andExpect(jsonPath("$[*].id").value(hasItem(assignmentId)));

        // 3. Lunes completo; el martes el maestro anota hasta las 6 p. m.
        //    (el token dura una hora: cada dia la app vuelve a iniciar sesion)
        clock.setTo(MON.atTime(17, 0));
        maestro = bearer(foreman.getEmail());
        send(post("/api/asignaciones/{id}/jornadas", assignmentId), maestro, day(MON, "16:00"))
                .andExpect(status().isCreated());
        clock.setTo(TUE.atTime(18, 30));
        maestro = bearer(foreman.getEmail());
        trabajador = bearer(worker.getEmail());
        String tuesday = idFrom(send(post("/api/asignaciones/{id}/jornadas", assignmentId), maestro,
                day(TUE, "18:00")).andExpect(status().isCreated()));

        // 4. El trabajador objeta el martes; el maestro corrige y el trabajador confirma todo
        send(post("/api/jornadas/{id}/objecion", tuesday), trabajador, Map.of("reason", "Sali a las 5:00 p. m."))
                .andExpect(status().isNoContent());
        send(put("/api/asignaciones/{id}/jornadas/{dayId}", assignmentId, tuesday), maestro, day(TUE, "17:00"))
                .andExpect(status().isNoContent());
        call(post("/api/mis-asignaciones/{id}/confirmacion-pendientes", assignmentId), trabajador)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // 5. Anticipo y liquidacion hasta el martes: 80.000 + (80.000 + 1 h extra de 12.500) - 50.000
        String advance = idFrom(send(post("/api/asignaciones/{id}/pagos", assignmentId), maestro,
                payment("ADVANCE", 50000, null)).andExpect(status().isCreated()));
        send(post("/api/asignaciones/{id}/pagos/cotizacion", assignmentId), maestro,
                payment("SETTLEMENT", null, TUE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(122500));
        String settlement = idFrom(send(post("/api/asignaciones/{id}/pagos", assignmentId), maestro,
                payment("SETTLEMENT", null, TUE.toString())).andExpect(status().isCreated()));

        // 6. El trabajador confirma los dos pagos y ve su comprobante
        call(post("/api/pagos/{id}/confirmacion", advance), trabajador).andExpect(status().isNoContent());
        call(post("/api/pagos/{id}/confirmacion", settlement), trabajador).andExpect(status().isNoContent());
        call(get("/api/pagos/{id}/comprobante", settlement), trabajador)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(122500));

        // 7. Un pago ya confirmado no se puede anular, y las cuentas quedan en cero
        send(post("/api/asignaciones/{id}/pagos/{paymentId}/anulacion", assignmentId, settlement), maestro,
                Map.of("reason", "Me equivoque de valor"))
                .andExpect(status().isUnprocessableEntity());
        call(get("/api/asignaciones/{id}", assignmentId), maestro)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.earnedConfirmed").value(172500))
                .andExpect(jsonPath("$.paidTotal").value(172500))
                .andExpect(jsonPath("$.balance").value(0));
        call(get("/api/mis-asignaciones/{id}", assignmentId), trabajador)
                .andExpect(jsonPath("$.balance").value(0));
    }

    // ================================================================== apoyo

    private Map<String, Object> day(LocalDate date, String checkOut) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("checkInTime", "07:00");
        body.put("checkOutTime", checkOut);
        body.put("breakMinutes", 60);
        body.put("taskPerformed", "Armado de hierro para columnas");
        return body;
    }

    private Map<String, Object> payment(String type, Integer amount, String periodEnd) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("method", "CASH");
        if (amount != null) {
            body.put("amount", amount);
            body.put("reason", "Pasajes de la semana");
        }
        if (periodEnd != null) {
            body.put("periodEnd", periodEnd);
        }
        return body;
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String bearer, Object body) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), bearer);
    }

    private String idFrom(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private String bearer(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", Scenario.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).get("accessToken").asText();
    }
}
