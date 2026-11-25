package com.labresource.platform.dashboard.controller;

import com.labresource.platform.dashboard.dto.InstitutionDashboardResponse;
import com.labresource.platform.dashboard.service.InstitutionDashboardService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/institution")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
public class InstitutionDashboardController {

    private final InstitutionDashboardService dashboardService;

    public InstitutionDashboardController(
            InstitutionDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public InstitutionDashboardResponse getDashboard(
            @RequestParam(required = false) Long institutionId,
            Authentication authentication
    ) {
        return dashboardService.getDashboard(
                authentication.getName(),
                institutionId
        );
    }
}