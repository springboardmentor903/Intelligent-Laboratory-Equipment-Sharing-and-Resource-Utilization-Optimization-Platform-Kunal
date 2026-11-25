
package com.labresource.platform.dashboard.controller;

import com.labresource.platform.dashboard.dto.ResearcherDashboardResponse;
import com.labresource.platform.dashboard.service.ResearcherDashboardService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class ResearcherDashboardController {

    private final ResearcherDashboardService dashboardService;

    public ResearcherDashboardController(
            ResearcherDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/researcher")
    @PreAuthorize("hasRole('RESEARCHER_STUDENT')")
    public ResearcherDashboardResponse getResearcherDashboard(
            Authentication authentication
    ) {
        return dashboardService.getDashboard(authentication.getName());
    }
}
