package com.jornal.controller.web;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.dto.form.FrontForm;
import com.jornal.dto.form.SiteForm;
import com.jornal.entity.Trade;
import com.jornal.exception.BusinessRuleException;
import com.jornal.security.JornalUserDetails;
import com.jornal.service.SiteAction;
import com.jornal.service.SiteService;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Locale;
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

/** Obras del maestro y apertura de frentes. */
@Controller
@RequestMapping("/maestro/obras")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("sites", siteService.listSites(me.id()));
        return "foreman/sites";
    }

    @GetMapping("/nueva")
    public String newSite(Model model) {
        model.addAttribute("form", new SiteForm());
        return "foreman/site-form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal JornalUserDetails me, @Valid @ModelAttribute("form") SiteForm form,
                         BindingResult result, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                UUID id = siteService.createSite(me.id(), form);
                Notice.success(redirect, "obra.creada");
                return "redirect:/maestro/obras/" + id;
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        return "foreman/site-form";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id, Model model) {
        model.addAttribute("site", siteService.getSite(me.id(), id));
        return "foreman/site-detail";
    }

    @GetMapping("/{id}/editar")
    public String edit(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id, Model model) {
        model.addAttribute("form", siteService.getSiteForm(me.id(), id));
        model.addAttribute("siteId", id);
        return "foreman/site-form";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                         @Valid @ModelAttribute("form") SiteForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                siteService.updateSite(me.id(), id, form);
                Notice.success(redirect, "obra.actualizada");
                return "redirect:/maestro/obras/" + id;
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        model.addAttribute("siteId", id);
        return "foreman/site-form";
    }

    @PostMapping("/{id}/estado")
    public String changeStatus(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                               @RequestParam SiteAction action, RedirectAttributes redirect) {
        siteService.changeStatus(me.id(), id, action);
        Notice.success(redirect, "obra.estado." + action.name().toLowerCase(Locale.ROOT));
        return "redirect:/maestro/obras/" + id;
    }

    @GetMapping("/{id}/frentes/nuevo")
    public String newFront(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id, Model model) {
        model.addAttribute("site", siteService.getSite(me.id(), id));
        model.addAttribute("form", new FrontForm());
        addFrontCatalogs(model);
        return "foreman/front-form";
    }

    @PostMapping("/{id}/frentes")
    public String openFront(@AuthenticationPrincipal JornalUserDetails me, @PathVariable UUID id,
                            @Valid @ModelAttribute("form") FrontForm form, BindingResult result, Model model,
                            RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                UUID frontId = siteService.openFront(me.id(), id, form);
                Notice.success(redirect, "frente.abierto");
                return "redirect:/maestro/frentes/" + frontId;
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        model.addAttribute("site", siteService.getSite(me.id(), id));
        addFrontCatalogs(model);
        return "foreman/front-form";
    }

    private static void addFrontCatalogs(Model model) {
        model.addAttribute("trades", Trade.values());
        model.addAttribute("daysOfWeek", EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY));
    }
}
