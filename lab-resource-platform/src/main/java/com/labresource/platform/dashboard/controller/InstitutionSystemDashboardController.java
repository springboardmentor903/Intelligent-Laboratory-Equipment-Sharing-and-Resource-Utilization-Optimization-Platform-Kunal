package com.labresource.platform.dashboard.controller;

import com.labresource.platform.dashboard.dto.InstitutionSystemDashboardResponse;
import com.labresource.platform.dashboard.service.InstitutionSystemDashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/institution/system")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
public class InstitutionSystemDashboardController {

    private final InstitutionSystemDashboardService service;

    public InstitutionSystemDashboardController(
            InstitutionSystemDashboardService service
    ) {
        this.service = service;
    }

    @GetMapping
    public InstitutionSystemDashboardResponse getDashboard(
            @RequestParam(required = false) Long institutionId,
            Authentication authentication
    ) {
        return service.getDashboard(
                authentication.getName(),
                institutionId
        );
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @RequestParam Long institutionId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        service.deactivateUser(
                authentication.getName(),
                institutionId,
                userId
        );

        return ResponseEntity.ok(
                Map.of("message", "User deactivated successfully")
        );
    }
}