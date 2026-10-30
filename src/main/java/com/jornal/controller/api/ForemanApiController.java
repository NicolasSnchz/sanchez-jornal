package com.jornal.controller.api;

import com.jornal.dto.api.IdResponse;
import com.jornal.dto.form.CompletionForm;
import com.jornal.dto.form.FrontForm;
import com.jornal.dto.form.InvitationForm;
import com.jornal.dto.form.PaymentForm;
import com.jornal.dto.form.ReasonForm;
import com.jornal.dto.form.SiteForm;
import com.jornal.dto.form.WorkDayForm;
import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.AssignmentRow;
import com.jornal.dto.view.CandidateRow;
import com.jornal.dto.view.FrontDetail;
import com.jornal.dto.view.PaymentRow;
import com.jornal.dto.view.SiteDetail;
import com.jornal.dto.view.SiteSummary;
import com.jornal.exception.ResourceNotFoundException;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
import com.jornal.service.PaymentService;
import com.jornal.service.SiteAction;
import com.jornal.service.SiteService;
import com.jornal.service.WorkDayService;
import com.jornal.strategy.PaymentQuote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del maestro. Los controladores solo traducen HTTP: validan la entrada con
 * {@code @Valid} y delegan en los mismos servicios que usan las vistas web.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Maestro", description = "Obras, frentes, cuadrilla, jornadas y pagos")
@SecurityRequirement(name = "bearerAuth")
public class ForemanApiController {

    private final SiteService siteService;
    private final AssignmentService assignmentService;
    private final WorkDayService workDayService;
    private final PaymentService paymentService;

    public ForemanApiController(SiteService siteService, AssignmentService assignmentService,
                                WorkDayService workDayService, PaymentService paymentService) {
        this.siteService = siteService;
        this.assignmentService = assignmentService;
        this.workDayService = workDayService;
        this.paymentService = paymentService;
    }

    // ------------------------------------------------------------ obras

    @GetMapping("/obras")
    @Operation(summary = "Listar mis obras")
    public List<SiteSummary> sites(@AuthenticationPrincipal JornalUserDetails me) {
        return siteService.listSites(me.id());
    }

    @PostMapping("/obras")
    @Operation(summary = "Crear una obra")
    @ApiResponse(responseCode = "201", description = "Creado; el header Location apunta al nuevo recurso")
    public ResponseEntity<IdResponse> createSite(@AuthenticationPrincipal JornalUserDetails me,
                                                 @Valid @RequestBody SiteForm form) {
        return created("/api/obras/", siteService.createSite(me.id(), form));
    }

    @GetMapping("/obras/{id}")
    @Operation(summary = "Detalle de una obra con sus frentes y asignaciones")
    public SiteDetail site(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id) {
        return siteService.getSite(me.id(), id);
    }

    @PostMapping("/obras/{id}/estado")
    @Operation(summary = "Iniciar, suspender, reanudar o finalizar una obra")
    @ApiResponse(responseCode = "204", description = "Hecho; la respuesta no tiene cuerpo")
    public ResponseEntity<Void> changeStatus(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                             @RequestParam("accion") SiteAction action) {
        siteService.changeStatus(me.id(), id, action);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/obras/{id}/frentes")
    @Operation(summary = "Abrir un frente de trabajo en una obra")
    @ApiResponse(responseCode = "201", description = "Creado; el header Location apunta al nuevo recurso")
    public ResponseEntity<IdResponse> openFront(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                                @Valid @RequestBody FrontForm form) {
        return created("/api/frentes/", siteService.openFront(me.id(), id, form));
    }

    // ------------------------------------------------------------ frentes

    @GetMapping("/frentes/{id}")
    @Operation(summary = "Detalle de un frente")
    public FrontDetail front(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id) {
        return siteService.getFront(me.id(), id);
    }

    @GetMapping("/frentes/{id}/candidatos")
    @Operation(summary = "Buscar trabajadores del oficio y la ciudad del frente, con su elegibilidad")
    public List<CandidateRow> candidates(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                         @RequestParam(name = "nombre", required = false) String name) {
        return assignmentService.findCandidates(me.id(), id, name);
    }

    @PostMapping("/frentes/{id}/invitaciones")
    @Operation(summary = "Invitar a un trabajador al frente")
    @ApiResponse(responseCode = "201", description = "Creado; el header Location apunta al nuevo recurso")
    public ResponseEntity<IdResponse> invite(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                             @Valid @RequestBody InvitationForm form) {
        return created("/api/asignaciones/", assignmentService.invite(me.id(), id, form));
    }

    // ------------------------------------------------------------ asignaciones

    @GetMapping("/asignaciones")
    @Operation(summary = "Cuadrilla: invitaciones abiertas y asignaciones en curso")
    public List<AssignmentRow> crew(@AuthenticationPrincipal JornalUserDetails me) {
        return assignmentService.listCrew(me.id());
    }

    @GetMapping("/asignaciones/{id}")
    @Operation(summary = "Planilla de una asignaci\u00f3n: semana, jornadas, pagos y saldo")
    public AssignmentDetail assignment(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                       @RequestParam(name = "semana", required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return assignmentService.getForForeman(me.id(), id, week);
    }

    @PostMapping("/asignaciones/{id}/cancelacion")
    @Operation(summary = "Cancelar una invitaci\u00f3n o una asignaci\u00f3n sin jornadas")
    @ApiResponse(responseCode = "204", description = "Hecho; la respuesta no tiene cuerpo")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                       @Valid @RequestBody ReasonForm form) {
        assignmentService.cancel(me.id(), id, form.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/asignaciones/{id}/finalizacion")
    @Operation(summary = "Finalizar la asignaci\u00f3n y calificar al trabajador")
    @ApiResponse(responseCode = "204", description = "Hecho; la respuesta no tiene cuerpo")
    public ResponseEntity<Void> complete(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                         @Valid @RequestBody CompletionForm form) {
        assignmentService.complete(me.id(), id, form);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ jornadas

    @PostMapping("/asignaciones/{id}/jornadas")
    @Operation(summary = "Registrar la jornada de un d\u00eda")
    @ApiResponse(responseCode = "201", description = "Creado; el header Location apunta al nuevo recurso")
    public ResponseEntity<IdResponse> registerWorkDay(@AuthenticationPrincipal JornalUserDetails me,
                                                      @PathVariable UUID id, @Valid @RequestBody WorkDayForm form) {
        UUID dayId = workDayService.registerWorkDay(me.id(), id, form);
        return created("/api/asignaciones/" + id + "/jornadas/", dayId);
    }

    @PutMapping("/asignaciones/{id}/jornadas/{dayId}")
    @Operation(summary = "Corregir una jornada por confirmar u objetada")
    @ApiResponse(responseCode = "204", description = "Hecho; la respuesta no tiene cuerpo")
    public ResponseEntity<Void> correctWorkDay(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                               @PathVariable UUID dayId, @Valid @RequestBody WorkDayForm form) {
        requireSameAssignment(id, workDayService.getForCorrection(me.id(), dayId).assignmentId());
        workDayService.correctWorkDay(me.id(), dayId, form);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ pagos

    @PostMapping("/asignaciones/{id}/pagos/cotizacion")
    @Operation(summary = "Calcular un pago sin registrarlo")
    public PaymentQuote quote(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                              @Valid @RequestBody PaymentForm form) {
        return paymentService.quote(me.id(), id, form);
    }

    @PostMapping("/asignaciones/{id}/pagos")
    @Operation(summary = "Registrar un anticipo, una liquidaci\u00f3n de periodo o la liquidaci\u00f3n final")
    @ApiResponse(responseCode = "201", description = "Creado; el header Location apunta al nuevo recurso")
    public ResponseEntity<PaymentRow> pay(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                          @Valid @RequestBody PaymentForm form) {
        PaymentRow payment = paymentService.register(me.id(), id, form);
        return ResponseEntity.created(URI.create("/api/asignaciones/" + id + "/pagos/" + payment.id())).body(payment);
    }

    @PostMapping("/asignaciones/{id}/pagos/{paymentId}/anulacion")
    @Operation(summary = "Anular un pago que el trabajador a\u00fan no ha confirmado")
    @ApiResponse(responseCode = "204", description = "Hecho; la respuesta no tiene cuerpo")
    public ResponseEntity<Void> cancelPayment(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                              @PathVariable UUID paymentId, @Valid @RequestBody ReasonForm form) {
        requireSameAssignment(id, paymentService.receiptForForeman(me.id(), paymentId).assignmentId());
        paymentService.cancel(me.id(), paymentId, form.getReason());
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<IdResponse> created(String basePath, UUID id) {
        return ResponseEntity.created(URI.create(basePath + id)).body(new IdResponse(id));
    }

    /** La URL anidada debe ser coherente: una jornada de otra asignacion se trata como inexistente. */
    private static void requireSameAssignment(UUID expected, UUID actual) {
        if (!expected.equals(actual)) {
            throw new ResourceNotFoundException("asignacion.no.encontrada");
        }
    }
}
