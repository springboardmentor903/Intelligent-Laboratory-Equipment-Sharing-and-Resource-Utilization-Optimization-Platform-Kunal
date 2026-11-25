
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.TechnicianWorkloadResponse;
import com.labresource.platform.maintenance.service.TechnicianWorkloadService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
@PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
public class TechnicianWorkloadController {

    private final TechnicianWorkloadService workloadService;

    public TechnicianWorkloadController(
            TechnicianWorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping("/workload")
    public ResponseEntity<List<TechnicianWorkloadResponse>> getWorkload(
            Authentication authentication) {

        return ResponseEntity.ok(
                workloadService.getInstitutionWorkload(
                        authentication.getName()
                )
        );
    }
}
