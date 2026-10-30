package com.jornal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jornal.entity.Assignment;
import com.jornal.entity.ConstructionSite;
import com.jornal.entity.Payment;
import com.jornal.entity.User;
import com.jornal.entity.WorkDay;
import com.jornal.entity.WorkDayStatus;
import com.jornal.entity.WorkFront;
import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.UserRepository;
import com.jornal.security.JornalUserDetails;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prueba de humo de la interfaz: con los datos de demostracion (que se crean con
 * las mismas reglas de dominio), cada pantalla de cada rol se debe renderizar
 * sin errores y sin textos faltantes ("??clave??" de Thymeleaf).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:paginas-${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.security.bcrypt-strength=4",
    "logging.level.com.jornal=INFO"
})
class PagesRenderIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConstructionSiteRepository siteRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private TransactionTemplate transaction;

    @TestFactory
    Stream<DynamicTest> cadaPantallaSeRenderizaCompleta() {
        List<DynamicTest> tests = new ArrayList<>();
        transaction.executeWithoutResult(status -> buildPages(tests));
        return tests.stream();
    }

    private void buildPages(List<DynamicTest> tests) {
        User carlos = userRepository.findByEmail("maestro@jornal.co").orElseThrow();
        User yeison = userRepository.findByEmail("trabajador@jornal.co").orElseThrow();
        User deiver = userRepository.findByEmail("deiver.carrillo@jornal.co").orElseThrow();
        User brayan = userRepository.findByEmail("brayan.contreras@jornal.co").orElseThrow();
        JornalUserDetails foreman = JornalUserDetails.from(carlos, LocalDateTime.now());
        JornalUserDetails worker = JornalUserDetails.from(yeison, LocalDateTime.now());
        JornalUserDetails invited = JornalUserDetails.from(deiver, LocalDateTime.now());

        Assignment yeisonA = assignmentRepository.findByWorkerIdOrderByInvitedAtDesc(yeison.getId()).get(0);
        Assignment deiverA = assignmentRepository.findByWorkerIdOrderByInvitedAtDesc(deiver.getId()).get(0);
        WorkFront slab = yeisonA.getFront();
        ConstructionSite house = slab.getSite();
        Payment payment = yeisonA.getPayments().get(0);
        WorkDay disputed = yeisonA.getWorkDays().stream()
                .filter(d -> d.getStatus() == WorkDayStatus.DISPUTED).findFirst().orElseThrow();
        ConstructionSite finished = siteRepository.findByForemanIdOrderByCreatedAtDesc(carlos.getId()).stream()
                .filter(s -> s.getStatus().name().equals("FINISHED")).findFirst().orElseThrow();

        page(tests, "login", get("/login"), "Ingresar");
        page(tests, "registro", get("/registro"), "Jornal");
        page(tests, "registro maestro", get("/registro/maestro"), "Cuenta de maestro");
        page(tests, "registro trabajador", get("/registro/trabajador"), "ARL");

        page(tests, "panel maestro", get("/maestro").with(user(foreman)), "Para resolver");
        page(tests, "obras", get("/maestro/obras").with(user(foreman)), house.getName());
        page(tests, "nueva obra", get("/maestro/obras/nueva").with(user(foreman)), "Nueva obra");
        page(tests, "detalle obra", get("/maestro/obras/{id}", house.getId()).with(user(foreman)), "Frentes de trabajo");
        page(tests, "obra finalizada", get("/maestro/obras/{id}", finished.getId()).with(user(foreman)), "Finalizada");
        page(tests, "editar obra", get("/maestro/obras/{id}/editar", house.getId()).with(user(foreman)), "Editar obra");
        page(tests, "nuevo frente", get("/maestro/obras/{id}/frentes/nuevo", house.getId()).with(user(foreman)), "Abrir frente");
        page(tests, "frente y candidatos", get("/maestro/frentes/{id}", slab.getId()).with(user(foreman)), "Buscar trabajadores");
        page(tests, "invitar", get("/maestro/frentes/{f}/invitar/{w}", slab.getId(), brayan.getId()).with(user(foreman)),
                "Enviar invitaci\u00f3n");
        page(tests, "cuadrilla", get("/maestro/cuadrilla").with(user(foreman)), "Cuadrilla");
        page(tests, "planilla maestro", get("/maestro/asignaciones/{id}", yeisonA.getId()).with(user(foreman)),
                "Planilla de la semana");
        page(tests, "planilla semana 1", get("/maestro/asignaciones/{id}", yeisonA.getId())
                .param("semana", yeisonA.getStartDate().toString()).with(user(foreman)), "Liquidado");
        page(tests, "registrar jornada", get("/maestro/asignaciones/{id}/jornadas/nueva", yeisonA.getId())
                .with(user(foreman)), "Registrar jornada");
        page(tests, "corregir jornada", get("/maestro/jornadas/{id}/editar", disputed.getId()).with(user(foreman)),
                "objet\u00f3");
        page(tests, "anticipo", get("/maestro/asignaciones/{id}/pagos/nuevo", yeisonA.getId()).param("tipo", "ADVANCE")
                .with(user(foreman)), "Anticipo");
        page(tests, "liquidacion", get("/maestro/asignaciones/{id}/pagos/nuevo", yeisonA.getId())
                .param("tipo", "SETTLEMENT").with(user(foreman)), "Liquidaci\u00f3n de periodo");
        page(tests, "liquidacion final", get("/maestro/asignaciones/{id}/pagos/nuevo", yeisonA.getId())
                .param("tipo", "FINAL_SETTLEMENT").with(user(foreman)), "Liquidaci\u00f3n final");
        page(tests, "finalizar", get("/maestro/asignaciones/{id}/finalizar", yeisonA.getId()).with(user(foreman)),
                "Finalizar asignaci\u00f3n");
        page(tests, "pagos del mes", get("/maestro/pagos").with(user(foreman)), "Total pagado");
        page(tests, "comprobante maestro", get("/maestro/pagos/{id}/comprobante", payment.getId()).with(user(foreman)),
                payment.getReceiptNumber());
        page(tests, "perfil maestro", get("/maestro/perfil").with(user(foreman)), "Actualizar datos");

        page(tests, "panel trabajador", get("/trabajador").with(user(worker)), "Te deben");
        page(tests, "mis trabajos", get("/trabajador/asignaciones").with(user(worker)), "Mis trabajos");
        page(tests, "planilla trabajador", get("/trabajador/asignaciones/{id}", yeisonA.getId()).with(user(worker)),
                "Todas las jornadas");
        page(tests, "comprobante trabajador", get("/trabajador/pagos/{id}/comprobante", payment.getId())
                .with(user(worker)), "Son:");
        page(tests, "perfil trabajador", get("/trabajador/perfil").with(user(worker)), "Recibir invitaciones");
        page(tests, "invitacion", get("/trabajador/asignaciones/{id}", deiverA.getId()).with(user(invited)),
                "Aceptar trabajo");
    }

    private void page(List<DynamicTest> tests, String name, MockHttpServletRequestBuilder request, String expected) {
        tests.add(DynamicTest.dynamicTest(name, () -> {
            MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
            String html = response.getContentAsString();

            assertThat(response.getStatus()).as(name).isEqualTo(200);
            assertThat(html).as("texto esperado en " + name).contains(expected);
            assertThat(html).as("mensajes faltantes en " + name).doesNotContain("??");
        }));
    }
}
