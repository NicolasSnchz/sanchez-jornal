package com.jornal.controller.api;

import com.jornal.dto.api.CountResponse;
import com.jornal.dto.api.RespondRequest;
import com.jornal.dto.form.RatingForm;
import com.jornal.dto.form.ReasonForm;
import com.jornal.dto.view.AssignmentDetail;
import com.jornal.dto.view.AssignmentRow;
import com.jornal.dto.view.Receipt;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
import com.jornal.service.PaymentService;
import com.jornal.service.WorkDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Trabajador", description = "Invitaciones, confirmacion de jornadas y de pagos")
@SecurityRequirement(name = "bearerAuth")
public class WorkerApiController {

    private final AssignmentService assignmentService;
    private final WorkDayService workDayService;
    private final PaymentService paymentService;

    public WorkerApiController(AssignmentService assignmentService, WorkDayService workDayService,
                               PaymentService paymentService) {
        this.assignmentService = assignmentService;
        this.workDayService = workDayService;
        this.paymentService = paymentService;
    }

    @GetMapping("/mis-asignaciones")
    @Operation(summary = "Mis asignaciones e invitaciones")
    public List<AssignmentRow> assignments(@AuthenticationPrincipal JornalUserDetails me) {
        return assignmentService.listForWorker(me.id());
    }

    @GetMapping("/mis-asignaciones/{id}")
    @Operation(summary = "Detalle de una asignacion: jornadas, pagos y saldo")
    public AssignmentDetail assignment(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                       @RequestParam(name = "semana", required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return assignmentService.getForWorker(me.id(), id, week);
    }

    @PostMapping("/mis-asignaciones/{id}/respuesta")
    @Operation(summary = "Aceptar o rechazar una invitacion")
    public ResponseEntity<Void> respond(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                        @Valid @RequestBody RespondRequest request) {
        assignmentService.respond(me.id(), id, request.accept());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mis-asignaciones/{id}/calificacion")
    @Operation(summary = "Calificar al maestro de una asignacion finalizada")
    public ResponseEntity<Void> rate(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                     @Valid @RequestBody RatingForm form) {
        assignmentService.rateForeman(me.id(), id, form.getRating());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mis-asignaciones/{id}/confirmacion-pendientes")
    @Operation(summary = "Confirmar todas las jornadas por confirmar")
    public CountResponse confirmAll(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id) {
        return new CountResponse(workDayService.confirmAllPending(me.id(), id));
    }

    @PostMapping("/jornadas/{id}/confirmacion")
    @Operation(summary = "Confirmar una jornada")
    public ResponseEntity<Void> confirm(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id) {
        workDayService.confirm(me.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jornadas/{id}/objecion")
    @Operation(summary = "Objetar una jornada con el motivo")
    public ResponseEntity<Void> dispute(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                                        @Valid @RequestBody ReasonForm form) {
        workDayService.dispute(me.id(), id, form.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pagos/{id}/confirmacion")
    @Operation(summary = "Confirmar que recibi un pago")
    public ResponseEntity<Void> confirmPayment(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id) {
        paymentService.confirmReceipt(me.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pagos/{id}/comprobante")
    @Operation(summary = "Comprobante de un pago")
    public Receipt receipt(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id) {
        return paymentService.receiptForWorker(me.id(), id);
    }
}
