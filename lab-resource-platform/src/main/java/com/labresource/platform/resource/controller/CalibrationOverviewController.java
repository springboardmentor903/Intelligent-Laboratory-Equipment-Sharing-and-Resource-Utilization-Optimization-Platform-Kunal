
package com.labresource.platform.resource.controller;

import com.labresource.platform.resource.dto.CalibrationOverviewResponse;
import com.labresource.platform.resource.service.CalibrationOverviewService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calibrations")
public class CalibrationOverviewController {

    private final CalibrationOverviewService calibrationOverviewService;

    public CalibrationOverviewController(
            CalibrationOverviewService calibrationOverviewService) {
        this.calibrationOverviewService = calibrationOverviewService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public Page<CalibrationOverviewResponse> getCalibrationOverview(
            Authentication authentication,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        return calibrationOverviewService.getOverview(
                authentication.getName(),
                departmentId,
                state,
                keyword,
                pageable
        );
    }
}
