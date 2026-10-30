package com.jornal.controller.web;

import com.jornal.controller.support.Notice;
import com.jornal.dto.form.RatingForm;
import com.jornal.dto.form.ReasonForm;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
import com.jornal.service.DashboardService;
import com.jornal.service.PaymentService;
import com.jornal.service.WorkDayService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vistas del trabajador, pensadas para el celular: ver su saldo, responder
 * invitaciones, confirmar u objetar jornadas y confirmar los pagos recibidos.
 */
@Controller
@RequestMapping("/trabajador")
public class WorkerController {

    private final DashboardService dashboardService;
    private final AssignmentService assignmentService;
    private final WorkDayService workDayService;
    private final PaymentService paymentService;

    public WorkerController(DashboardService dashboardService, AssignmentService assignmentService,
                            WorkDayService workDayService, PaymentService paymentService) {
        this.dashboardService = dashboardService;
        this.assignmentService = assignmentService;
        this.workDayService = workDayService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("dashboard", dashboardService.workerDashboard(me.id()));
        return "worker/dashboard";
    }

    @GetMapping("/asignaciones")
    public String assignments(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("assignments", assignmentService.listForWorker(me.id()));
        return "worker/assignments";
    }

    @GetMapping("/asignaciones/{id}")
    public String detail(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                         @RequestParam(name = "semana", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                         Model model) {
        model.addAttribute("a", assignmentService.getForWorker(me.id(), id, week));
        model.addAttribute("reasonForm", new ReasonForm());
        model.addAttribute("ratingForm", new RatingForm());
        return "worker/assignment-detail";
    }

    @PostMapping("/asignaciones/{id}/respuesta")
    public String respond(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                          @RequestParam("aceptar") boolean accept, RedirectAttributes redirect) {
        assignmentService.respond(me.id(), id, accept);
        Notice.success(redirect, accept ? "invitacion.aceptada" : "invitacion.rechazada");
        return accept ? "redirect:/trabajador/asignaciones/" + id : "redirect:/trabajador";
    }

    @PostMapping("/asignaciones/{id}/calificar")
    public String rate(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                       @Valid @ModelAttribute("ratingForm") RatingForm form, BindingResult result,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            Notice.error(redirect, "validacion.calificacion");
        } else {
            assignmentService.rateForeman(me.id(), id, form.getRating());
            Notice.success(redirect, "calificacion.enviada");
        }
        return "redirect:/trabajador/asignaciones/" + id;
    }

    @PostMapping("/jornadas/{dayId}/confirmar")
    public String confirmWorkDay(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID dayId,
                                 RedirectAttributes redirect) {
        UUID assignmentId = workDayService.confirm(me.id(), dayId);
        Notice.success(redirect, "jornada.confirmada");
        return "redirect:/trabajador/asignaciones/" + assignmentId + "#jornadas";
    }

    @PostMapping("/asignaciones/{id}/confirmar-pendientes")
    public String confirmAll(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                             RedirectAttributes redirect) {
        int confirmed = workDayService.confirmAllPending(me.id(), id);
        Notice.success(redirect, "jornadas.confirmadas", confirmed);
        return "redirect:/trabajador/asignaciones/" + id + "#jornadas";
    }

    @PostMapping("/jornadas/{dayId}/objetar")
    public String dispute(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID dayId,
                          @Valid @ModelAttribute("reasonForm") ReasonForm form, BindingResult result,
                          @RequestParam("asignacion") UUID assignmentId, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            Notice.error(redirect, "validacion.motivo");
            return "redirect:/trabajador/asignaciones/" + assignmentId + "#jornadas";
        }
        UUID owner = workDayService.dispute(me.id(), dayId, form.getReason());
        Notice.success(redirect, "jornada.objetada");
        return "redirect:/trabajador/asignaciones/" + owner + "#jornadas";
    }

    @PostMapping("/pagos/{paymentId}/confirmar")
    public String confirmPayment(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID paymentId,
                                 RedirectAttributes redirect) {
        UUID assignmentId = paymentService.confirmReceipt(me.id(), paymentId);
        Notice.success(redirect, "pago.confirmado");
        return "redirect:/trabajador/asignaciones/" + assignmentId + "#pagos";
    }

    @GetMapping("/pagos/{paymentId}/comprobante")
    public String receipt(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID paymentId, Model model) {
        model.addAttribute("receipt", paymentService.receiptForWorker(me.id(), paymentId));
        return "shared/receipt";
    }
}
