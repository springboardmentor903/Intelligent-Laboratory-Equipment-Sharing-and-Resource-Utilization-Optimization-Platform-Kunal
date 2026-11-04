
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import com.labresource.platform.maintenance.service.MaintenanceRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceService;

    public MaintenanceRequestController(
            MaintenanceRequestService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceService.getAllRequests();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public MaintenanceRequest getRequestById(@PathVariable Long id) {
        return maintenanceService.getRequestById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public MaintenanceRequest createRequest(@RequestBody Map<String, String> body) {
        Long resourceId = Long.valueOf(body.get("resourceId"));
        String description = body.get("description");

        LocalDateTime scheduledStart = body.get("scheduledStart") == null
                || body.get("scheduledStart").isBlank()
                ? null : LocalDateTime.parse(body.get("scheduledStart"));

        LocalDateTime scheduledEnd = body.get("scheduledEnd") == null
                || body.get("scheduledEnd").isBlank()
                ? null : LocalDateTime.parse(body.get("scheduledEnd"));

        return maintenanceService.createRequest(
                resourceId, description, scheduledStart, scheduledEnd);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<MaintenanceRequest> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        MaintenanceRequest updated = maintenanceService.updateStatus(
                id, body.get("status"), body.get("resolutionNotes"));

        return ResponseEntity.ok(updated);
    }
}
