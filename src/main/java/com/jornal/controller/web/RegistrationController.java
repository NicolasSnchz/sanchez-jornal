package com.jornal.controller.web;

import com.jornal.controller.support.MessageArgs;
import com.jornal.controller.support.Notice;
import com.jornal.dto.form.ForemanRegistrationForm;
import com.jornal.dto.form.PersonalDataForm;
import com.jornal.dto.form.WorkerRegistrationForm;
import com.jornal.entity.DocumentType;
import com.jornal.entity.Gender;
import com.jornal.entity.Trade;
import com.jornal.exception.BusinessRuleException;
import com.jornal.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/registro")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @ModelAttribute
    public void catalogs(Model model) {
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("trades", Trade.values());
    }

    @GetMapping
    public String chooseRole() {
        return "auth/register";
    }

    @GetMapping("/maestro")
    public String foremanForm(Model model) {
        model.addAttribute("form", new ForemanRegistrationForm());
        return "auth/register-foreman";
    }

    @PostMapping("/maestro")
    public String registerForeman(@Valid @ModelAttribute("form") ForemanRegistrationForm form, BindingResult result,
                                  RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                registrationService.registerForeman(form);
                Notice.success(redirect, "registro.exitoso");
                return "redirect:/login";
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        clearPasswords(form);
        return "auth/register-foreman";
    }

    @GetMapping("/trabajador")
    public String workerForm(Model model) {
        model.addAttribute("form", new WorkerRegistrationForm());
        return "auth/register-worker";
    }

    @PostMapping("/trabajador")
    public String registerWorker(@Valid @ModelAttribute("form") WorkerRegistrationForm form, BindingResult result,
                                 RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                registrationService.registerWorker(form);
                Notice.success(redirect, "registro.exitoso");
                return "redirect:/login";
            } catch (BusinessRuleException e) {
                result.reject(e.getCode(), MessageArgs.format(e.getArgs()), null);
            }
        }
        clearPasswords(form);
        return "auth/register-worker";
    }

    /** La clave nunca vuelve al navegador dentro del HTML del formulario. */
    private static void clearPasswords(PersonalDataForm form) {
        form.setPassword(null);
        form.setPasswordConfirmation(null);
    }
}
