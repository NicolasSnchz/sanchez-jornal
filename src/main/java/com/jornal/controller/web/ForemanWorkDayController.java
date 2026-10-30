package com.jornal.controller.web;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.dto.form.WorkDayForm;
import com.jornal.dto.view.WorkDayCorrection;
import com.jornal.exception.BusinessRuleException;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
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

/** Registro y correccion de jornadas por parte del maestro. */
@Controller
@RequestMapping("/maestro")
public class ForemanWorkDayController {

    private final WorkDayService workDayService;
    private final AssignmentService assignmentService;

    public ForemanWorkDayController(WorkDayService workDayService, AssignmentService assignmentService) {
        this.workDayService = workDayService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/asignaciones/{id}/jornadas/nueva")
    public String newWorkDay(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                             @RequestParam(name = "fecha", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             Model model) {
        WorkDayForm form = workDayService.prepareWorkDay(me.id(), id, date);
        model.addAttribute("form", form);
        model.addAttribute("a", assignmentService.getForForeman(me.id(), id, form.getDate()));
        return "foreman/workday-form";
    }

    @PostMapping("/asignaciones/{id}/jornadas")
    public String register(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                           @Valid @ModelAttribute("form") WorkDayForm form, BindingResult result, Model model,
                           RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                workDayService.registerWorkDay(me.id(), id, form);
                Notice.success(redirect, "jornada.registrada", form.getDate());
                return "redirect:/maestro/asignaciones/" + id + "?semana=" + form.getDate();
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        model.addAttribute("a", assignmentService.getForForeman(me.id(), id, form.getDate()));
        return "foreman/workday-form";
    }

    @GetMapping("/jornadas/{dayId}/editar")
    public String edit(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID dayId, Model model) {
        WorkDayCorrection correction = workDayService.getForCorrection(me.id(), dayId);
        model.addAttribute("correction", correction);
        model.addAttribute("form", correction.form());
        model.addAttribute("a", assignmentService.getForForeman(me.id(), correction.assignmentId(),
                correction.current().date()));
        return "foreman/workday-form";
    }

    @PostMapping("/jornadas/{dayId}")
    public String correct(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID dayId,
                          @Valid @ModelAttribute("form") WorkDayForm form, BindingResult result, Model model,
                          RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                UUID assignmentId = workDayService.correctWorkDay(me.id(), dayId, form);
                Notice.success(redirect, "jornada.corregida");
                return "redirect:/maestro/asignaciones/" + assignmentId + "?semana=" + form.getDate();
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        WorkDayCorrection correction = workDayService.getForCorrection(me.id(), dayId);
        model.addAttribute("correction", correction);
        model.addAttribute("a", assignmentService.getForForeman(me.id(), correction.assignmentId(),
                correction.current().date()));
        return "foreman/workday-form";
    }
}
