package com.labresource.platform.report.analytics;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ResourceAnalyticsController {

    private final ResourceAnalyticsService analyticsService;

    public ResourceAnalyticsController(
            ResourceAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('LAB_MANAGER','DEPARTMENT_HEAD','INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    public ResourceAnalyticsResponse getAnalytics(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            org.springframework.security.core.Authentication authentication) {

        LocalDate today = LocalDate.now();

        if (fromDate == null) {
            fromDate = today.minusDays(6);
        }

        if (toDate == null) {
            toDate = today;
        }

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException(
                    "To date cannot be before from date");
        }

        if (fromDate.isBefore(today.minusYears(2))) {
            throw new IllegalArgumentException(
                    "Maximum report range is two years");
        }

        return analyticsService.getAnalytics(
                fromDate,
                toDate,
                authentication.getName()
        );
    }
}