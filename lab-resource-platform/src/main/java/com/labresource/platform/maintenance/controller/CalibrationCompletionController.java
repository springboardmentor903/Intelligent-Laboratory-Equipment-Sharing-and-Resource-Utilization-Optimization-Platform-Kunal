
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.CalibrationCompletionRequest;
import com.labresource.platform.maintenance.dto.CalibrationCompletionResponse;
import com.labresource.platform.maintenance.service.CalibrationCompletionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-orders")
public class CalibrationCompletionController {

    private final CalibrationCompletionService completionService;

    public CalibrationCompletionController(
            CalibrationCompletionService completionService) {
        this.completionService = completionService;
    }

    @PatchMapping("/{id}/complete-calibration")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'LAB_TECHNICIAN')")
    public ResponseEntity<CalibrationCompletionResponse> completeCalibration(
            @PathVariable Long id,
            @Valid @RequestBody CalibrationCompletionRequest request,
            Authentication authentication) {

        CalibrationCompletionResponse response =
                completionService.completeCalibration(
                        id,
                        request,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }
}
