package com.jornal.controller.web;

import com.jornal.security.JornalUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos que necesita la plantilla base en todas las paginas: usuario, ruta actual y fecha de hoy. */
@ControllerAdvice(basePackages = "com.jornal.controller.web")
public class LayoutAdvice {

    private final Clock clock;

    public LayoutAdvice(Clock clock) {
        this.clock = clock;
    }

    @ModelAttribute("me")
    public JornalUserDetails me(@AuthenticationPrincipal JornalUserDetails user) {
        return user;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
