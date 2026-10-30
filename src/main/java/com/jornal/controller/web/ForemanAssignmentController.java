package com.jornal.controller.web;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.dto.form.CompletionForm;
import com.jornal.dto.form.ReasonForm;
import com.jornal.exception.BusinessRuleException;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
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

/** Planilla de una asignacion vista por el maestro, su cuadrilla y el cierre de asignaciones. */
@Controller
@RequestMapping("/maestro")
public class ForemanAssignmentController {

    private final AssignmentService assignmentService;

    public ForemanAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/cuadrilla")
    public String crew(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("crew", assignmentService.listCrew(me.id()));
        return "foreman/crew";
    }

    @GetMapping("/asignaciones/{id}")
    public String detail(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                         @RequestParam(name = "semana", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                         Model model) {
        model.addAttribute("a", assignmentService.getForForeman(me.id(), id, week));
        model.addAttribute("reasonForm", new ReasonForm());
        return "foreman/assignment-detail";
    }

    @PostMapping("/asignaciones/{id}/cancelar")
    public String cancel(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                         @Valid @ModelAttribute("reasonForm") ReasonForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            Notice.error(redirect, "validacion.motivo");
        } else {
            assignmentService.cancel(me.id(), id, form.getReason());
            Notice.success(redirect, "asignacion.cancelada");
        }
        return "redirect:/maestro/asignaciones/" + id;
    }

    @GetMapping("/asignaciones/{id}/finalizar")
    public String completion(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id, Model model) {
        model.addAttribute("a", assignmentService.getForForeman(me.id(), id, null));
        model.addAttribute("form", new CompletionForm());
        return "foreman/completion-form";
    }

    @PostMapping("/asignaciones/{id}/finalizar")
    public String complete(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                           @Valid @ModelAttribute("form") CompletionForm form, BindingResult result, Model model,
                           RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                assignmentService.complete(me.id(), id, form);
                Notice.success(redirect, "asignacion.finalizada");
                return "redirect:/maestro/asignaciones/" + id;
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        model.addAttribute("a", assignmentService.getForForeman(me.id(), id, null));
        return "foreman/completion-form";
    }
}
