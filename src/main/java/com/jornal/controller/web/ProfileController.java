package com.jornal.controller.web;

import com.jornal.controller.support.Notice;
import com.jornal.dto.form.ForemanProfileForm;
import com.jornal.dto.form.WorkerProfileForm;
import com.jornal.entity.Trade;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/maestro/perfil")
    public String foremanProfile(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("form", profileService.getForemanProfile(me.id()));
        model.addAttribute("profile", profileService.summary(me.id()));
        return "shared/profile";
    }

    @PostMapping("/maestro/perfil")
    public String updateForeman(@AuthenticationPrincipal JornalUserDetails me,
                                @Valid @ModelAttribute("form") ForemanProfileForm form, BindingResult result,
                                Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("profile", profileService.summary(me.id()));
            return "shared/profile";
        }
        profileService.updateForemanProfile(me.id(), form);
        Notice.success(redirect, "perfil.actualizado");
        return "redirect:/maestro/perfil";
    }

    @GetMapping("/trabajador/perfil")
    public String workerProfile(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("form", profileService.getWorkerProfile(me.id()));
        model.addAttribute("profile", profileService.summary(me.id()));
        model.addAttribute("trades", Trade.values());
        return "shared/profile";
    }

    @PostMapping("/trabajador/perfil")
    public String updateWorker(@AuthenticationPrincipal JornalUserDetails me,
                               @Valid @ModelAttribute("form") WorkerProfileForm form, BindingResult result,
                               Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("profile", profileService.summary(me.id()));
            model.addAttribute("trades", Trade.values());
            return "shared/profile";
        }
        profileService.updateWorkerProfile(me.id(), form);
        Notice.success(redirect, "perfil.actualizado");
        return "redirect:/trabajador/perfil";
    }
}
