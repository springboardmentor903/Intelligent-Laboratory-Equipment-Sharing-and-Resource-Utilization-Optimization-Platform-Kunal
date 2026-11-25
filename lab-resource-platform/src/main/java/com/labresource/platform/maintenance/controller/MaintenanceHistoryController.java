
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.MaintenanceHistoryResponse;
import com.labresource.platform.maintenance.service.MaintenanceHistoryService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance/history")
@PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'LAB_TECHNICIAN')")
public class MaintenanceHistoryController {

    private final MaintenanceHistoryService maintenanceHistoryService;

    public MaintenanceHistoryController(
            MaintenanceHistoryService maintenanceHistoryService) {
        this.maintenanceHistoryService = maintenanceHistoryService;
    }

    @GetMapping
    public List<MaintenanceHistoryResponse> getMaintenanceHistory(
            Principal principal,

            @RequestParam(required = false)
            Long resourceId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        return maintenanceHistoryService.getMaintenanceHistory(
                principal.getName(),
                resourceId,
                from,
                to
        );
    }
}
