
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.TechnicianTaskResponse;
import com.labresource.platform.maintenance.dto.TechnicianTaskSummaryResponse;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.service.TechnicianTaskService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technician")
@PreAuthorize("hasRole('LAB_TECHNICIAN')")
public class TechnicianTaskController {

    private final TechnicianTaskService taskService;

    public TechnicianTaskController(TechnicianTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<Page<TechnicianTaskResponse>> getMyTasks(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "scheduledEnd")
            Pageable pageable,
            Authentication authentication) {

        return ResponseEntity.ok(
                taskService.getMyTasks(
                        authentication.getName(),
                        status,
                        departmentId,
                        keyword,
                        pageable
                )
        );
    }

    @GetMapping("/my-tasks/summary")
    public ResponseEntity<TechnicianTaskSummaryResponse> getMyTaskSummary(
            Authentication authentication) {

        return ResponseEntity.ok(
                taskService.getMyTaskSummary(authentication.getName())
        );
    }
}
