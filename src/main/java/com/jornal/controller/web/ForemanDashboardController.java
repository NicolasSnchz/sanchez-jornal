package com.jornal.controller.web;

import com.jornal.security.JornalUserDetails;
import com.jornal.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForemanDashboardController {

    private final DashboardService dashboardService;

    public ForemanDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/maestro")
    public String dashboard(@AuthenticationPrincipal JornalUserDetails me, Model model) {
        model.addAttribute("dashboard", dashboardService.foremanDashboard(me.id()));
        return "foreman/dashboard";
    }
}
