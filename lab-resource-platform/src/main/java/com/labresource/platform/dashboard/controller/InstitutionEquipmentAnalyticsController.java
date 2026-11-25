package com.labresource.platform.dashboard.controller;

import com.labresource.platform.dashboard.dto.ProcurementRecommendationResponse;
import com.labresource.platform.dashboard.dto.EquipmentLifecycleResponse;
import com.labresource.platform.dashboard.service.InstitutionEquipmentAnalyticsService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/dashboard/institution")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
public class InstitutionEquipmentAnalyticsController {

    private final InstitutionEquipmentAnalyticsService service;

    public InstitutionEquipmentAnalyticsController(
            InstitutionEquipmentAnalyticsService service
    ) {
        this.service = service;
    }

    @GetMapping("/procurement")
    public ProcurementRecommendationResponse procurement(
            @RequestParam Long institutionId,
            @RequestParam(required = false) BigDecimal highDemandThreshold,
            @RequestParam(required = false) BigDecimal lowUtilizationThreshold,
            @RequestParam(required = false) BigDecimal maintenanceThreshold,
            Authentication authentication
    ) {
        return service.getProcurementRecommendations(
                institutionId,
                highDemandThreshold,
                lowUtilizationThreshold,
                maintenanceThreshold
        );
    }

    @GetMapping("/lifecycle")
    public EquipmentLifecycleResponse lifecycle(
            @RequestParam Long institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return service.getLifecycle(institutionId, page, size);
    }
}