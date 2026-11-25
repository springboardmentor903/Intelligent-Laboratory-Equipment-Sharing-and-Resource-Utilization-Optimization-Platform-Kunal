
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.DowntimeRecordResponse;
import com.labresource.platform.maintenance.service.DowntimeService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance/downtime")
public class DowntimeController {

    private final DowntimeService downtimeService;

    public DowntimeController(DowntimeService downtimeService) {
        this.downtimeService = downtimeService;
    }

    // INSTITUTION DOWNTIME - LAB MANAGER

    @GetMapping("/institution")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public ResponseEntity<List<DowntimeRecordResponse>>
    getInstitutionDowntime(

            @RequestParam(required = false) Long resourceId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            java.security.Principal principal) {

        return ResponseEntity.ok(
                downtimeService.getInstitutionDowntime(
                        principal.getName(),
                        resourceId,
                        from,
                        to
                )
        );
    }

    // PERSONAL DOWNTIME - LAB TECHNICIAN

    @GetMapping("/my")
    @PreAuthorize("hasRole('LAB_TECHNICIAN')")
    public ResponseEntity<List<DowntimeRecordResponse>>
    getMyDowntime(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            java.security.Principal principal) {

        return ResponseEntity.ok(
                downtimeService.getMyDowntime(
                        principal.getName(),
                        from,
                        to
                )
        );
    }
}
