package com.jornal.controller.web;

import com.jornal.security.JornalUserDetails;
import com.jornal.security.RoleBasedSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class HomeController {

    private final boolean demoAccounts;

    /** En dev la pagina de ingreso muestra las cuentas de demostracion; en prod nunca. */
    public HomeController(@Value("${app.seed.enabled:false}") boolean demoAccounts) {
        this.demoAccounts = demoAccounts;
    }

    /** La raiz lleva a cada quien a su panel. */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal JornalUserDetails user) {
        return "redirect:" + RoleBasedSuccessHandler.homeOf(user);
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal JornalUserDetails user, Model model) {
        if (user != null) {
            return "redirect:" + RoleBasedSuccessHandler.homeOf(user);
        }
        model.addAttribute("demoAccounts", demoAccounts);
        return "auth/login";
    }

    /** Destino de accessDeniedPage: Spring Security reenvia aqui con cualquier metodo HTTP. */
    @RequestMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden() {
        return "error/403";
    }
}
