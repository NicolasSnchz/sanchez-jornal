package com.jornal.controller.web;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.dto.form.InvitationForm;
import com.jornal.dto.view.CandidateRow;
import com.jornal.dto.view.FrontDetail;
import com.jornal.exception.BusinessRuleException;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.AssignmentService;
import com.jornal.service.SiteService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
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

/** Frente de trabajo: su cuadrilla, la busqueda de candidatos y las invitaciones. */
@Controller
@RequestMapping("/maestro/frentes/{frontId}")
public class FrontController {

    private final SiteService siteService;
    private final AssignmentService assignmentService;
    private final Clock clock;

    public FrontController(SiteService siteService, AssignmentService assignmentService, Clock clock) {
        this.siteService = siteService;
        this.assignmentService = assignmentService;
        this.clock = clock;
    }

    @GetMapping
    public String detail(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID frontId,
                         @RequestParam(name = "nombre", required = false) String name, Model model) {
        FrontDetail front = siteService.getFront(me.id(), frontId);
        model.addAttribute("detail", front);
        model.addAttribute("name", name);
        if (front.front().canInvite()) {
            model.addAttribute("candidates", assignmentService.findCandidates(me.id(), frontId, name));
        }
        return "foreman/front-detail";
    }

    @GetMapping("/invitar/{workerId}")
    public String invitation(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID frontId,
                             @PathVariable UUID workerId, Model model) {
        FrontDetail front = siteService.getFront(me.id(), frontId);
        InvitationForm form = new InvitationForm();
        form.setWorkerId(workerId);
        form.setDailyWage(front.front().offeredDailyWage());
        LocalDate today = LocalDate.now(clock);
        form.setStartDate(front.front().startDate().isAfter(today) ? front.front().startDate() : today);
        return invitationView(me, front, workerId, form, model);
    }

    @PostMapping("/invitaciones")
    public String invite(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID frontId,
                         @Valid @ModelAttribute("form") InvitationForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (form.getWorkerId() == null) {
            return "redirect:/maestro/frentes/" + frontId;
        }
        if (!result.hasErrors()) {
            try {
                assignmentService.invite(me.id(), frontId, form);
                Notice.success(redirect, "invitacion.enviada");
                return "redirect:/maestro/frentes/" + frontId;
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        return invitationView(me, siteService.getFront(me.id(), frontId), form.getWorkerId(), form, model);
    }

    private String invitationView(JornalUserDetails me, FrontDetail front, UUID workerId, InvitationForm form,
                                  Model model) {
        CandidateRow candidate = assignmentService.getCandidate(me.id(), front.front().id(), workerId);
        model.addAttribute("detail", front);
        model.addAttribute("candidate", candidate);
        model.addAttribute("form", form);
        return "foreman/invitation-form";
    }
}
