
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.CreatePreventiveScheduleRequest;
import com.labresource.platform.maintenance.dto.PreventiveScheduleResponse;
import com.labresource.platform.maintenance.service.PreventiveMaintenanceScheduleService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance/schedules")
@PreAuthorize("hasRole('LAB_MANAGER')")
public class PreventiveMaintenanceScheduleController {

    private final PreventiveMaintenanceScheduleService scheduleService;

    public PreventiveMaintenanceScheduleController(
            PreventiveMaintenanceScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // CREATE PREVENTIVE SCHEDULE

    @PostMapping
    public ResponseEntity<PreventiveScheduleResponse> createSchedule(
            @Valid @RequestBody CreatePreventiveScheduleRequest request,
            Authentication authentication) {

        PreventiveScheduleResponse response =
                scheduleService.createSchedule(
                        request,
                        authentication.getName()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET ALL INSTITUTION SCHEDULES

    @GetMapping
    public ResponseEntity<List<PreventiveScheduleResponse>>
    getInstitutionSchedules(Authentication authentication) {

        return ResponseEntity.ok(
                scheduleService.getInstitutionSchedules(
                        authentication.getName()
                )
        );
    }

    // GET EQUIPMENT-WISE SCHEDULES

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<PreventiveScheduleResponse>>
    getResourceSchedules(
            @PathVariable Long resourceId,
            Authentication authentication) {

        return ResponseEntity.ok(
                scheduleService.getResourceSchedules(
                        resourceId,
                        authentication.getName()
                )
        );
    }

    // GET SINGLE SCHEDULE

    @GetMapping("/{id}")
    public ResponseEntity<PreventiveScheduleResponse> getSchedule(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        id,
                        authentication.getName()
                )
        );
    }

    // UPDATE SCHEDULE

    @PutMapping("/{id}")
    public ResponseEntity<PreventiveScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody CreatePreventiveScheduleRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                scheduleService.updateSchedule(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    // ACTIVATE / DEACTIVATE SCHEDULE

    @PatchMapping("/{id}/status")
    public ResponseEntity<PreventiveScheduleResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {

        if (request == null || request.get("active") == null) {
            throw new IllegalArgumentException(
                    "Active status is required");
        }

        return ResponseEntity.ok(
                scheduleService.updateScheduleStatus(
                        id,
                        request.get("active"),
                        authentication.getName()
                )
        );
    }
}
