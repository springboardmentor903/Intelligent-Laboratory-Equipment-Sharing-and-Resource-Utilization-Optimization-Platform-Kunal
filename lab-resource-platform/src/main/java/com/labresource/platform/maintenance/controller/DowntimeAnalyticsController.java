
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.DowntimeAnalyticsResponse;
import com.labresource.platform.maintenance.service.DowntimeAnalyticsService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/maintenance/analytics")
@PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN')")
public class DowntimeAnalyticsController {

    private final DowntimeAnalyticsService downtimeAnalyticsService;

    public DowntimeAnalyticsController(
            DowntimeAnalyticsService downtimeAnalyticsService) {
        this.downtimeAnalyticsService = downtimeAnalyticsService;
    }

    @GetMapping("/downtime")
    public DowntimeAnalyticsResponse getDowntimeAnalytics(
            Principal principal,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        return downtimeAnalyticsService.getInstitutionDowntimeAnalytics(
                principal.getName(),
                from,
                to
        );
    }
}
