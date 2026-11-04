package com.labresource.platform.sharing.analytics;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sharing-analytics")
public class SharingAnalyticsController {

    private final SharingAnalyticsService analyticsService;

    public SharingAnalyticsController(
            SharingAnalyticsService analyticsService
    ) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','INSTITUTION_ADMIN','LAB_MANAGER','DEPARTMENT_HEAD')")
    public SharingAnalyticsResponse getAnalytics(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            Authentication authentication
    ) {

        LocalDate today = LocalDate.now();

        if (fromDate == null) {
            fromDate = today.minusDays(29);
        }

        if (toDate == null) {
            toDate = today;
        }

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException(
                    "To date cannot be before from date"
            );
        }

        if (fromDate.isBefore(today.minusYears(2))) {
            throw new IllegalArgumentException(
                    "Maximum report range is two years"
            );
        }

        return analyticsService.getAnalytics(
                fromDate,
                toDate,
                authentication.getName()
        );
    }
}