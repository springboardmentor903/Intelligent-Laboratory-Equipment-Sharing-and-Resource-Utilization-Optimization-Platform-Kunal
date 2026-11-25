
package com.labresource.platform.maintenance.controller;

import com.labresource.platform.maintenance.dto.MaintenanceRequestResponse;
import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import com.labresource.platform.maintenance.service.MaintenanceRequestService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceService;

    public MaintenanceRequestController(
            MaintenanceRequestService maintenanceService
    ) {
        this.maintenanceService = maintenanceService;
    }

    public record CreateRequest(
            Long resourceId,
            String title,
            String description,
            String priority
    ) {}

    public record ReviewRequest(String remarks) {}

    private String email(Authentication authentication) {
        return authentication.getName();
    }

    private Pageable pageable(
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        String requestedSort = sortBy == null ? "createdAt" : sortBy;

        String safeSort = switch (requestedSort) {
            case "createdAt", "updatedAt", "priority", "status", "id" ->
                    requestedSort;
            default -> "createdAt";
        };

        Sort.Direction sortDirection =
                "ASC".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        return PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, safeSort)
        );
    }

    // CREATE REQUEST

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'LAB_TECHNICIAN')")
    @Transactional
    public MaintenanceRequestResponse createRequest(
            @RequestBody CreateRequest body,
            Authentication authentication
    ) {
        if (body == null) {
            throw new IllegalArgumentException(
                    "Request body is required");
        }

        MaintenanceRequest saved = maintenanceService.createRequest(
                email(authentication),
                body.resourceId(),
                body.title(),
                body.description(),
                body.priority()
        );

        return MaintenanceRequestResponse.from(saved);
    }

    // MY REQUESTS

    @GetMapping("/requests/my")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'LAB_TECHNICIAN')")
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponse> getMyRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            Authentication authentication
    ) {
        Page<MaintenanceRequest> requests =
                maintenanceService.getMyRequests(
                        email(authentication),
                        status,
                        resourceId,
                        priority,
                        pageable(page, size, sortBy, direction)
                );

        return requests.map(MaintenanceRequestResponse::from);
    }

    // MANAGER QUEUE

    @GetMapping("/requests/queue")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponse> getManagerQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            Authentication authentication
    ) {
        Page<MaintenanceRequest> requests =
                maintenanceService.getManagerQueue(
                        email(authentication),
                        status,
                        resourceId,
                        priority,
                        pageable(page, size, sortBy, direction)
                );

        return requests.map(MaintenanceRequestResponse::from);
    }

    // VIEW OWN REQUEST

    @GetMapping("/requests/my/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER_STUDENT', 'DEPARTMENT_HEAD', 'LAB_TECHNICIAN')")
    @Transactional(readOnly = true)
    public MaintenanceRequestResponse getMyRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        MaintenanceRequest request =
                maintenanceService.getMyRequestById(
                        email(authentication),
                        id
                );

        return MaintenanceRequestResponse.from(request);
    }

    // APPROVE REQUEST

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    @Transactional
    public MaintenanceRequestResponse approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequest body,
            Authentication authentication
    ) {
        MaintenanceRequest request =
                maintenanceService.reviewRequest(
                        email(authentication),
                        id,
                        "APPROVED",
                        body == null ? null : body.remarks()
                );

        return MaintenanceRequestResponse.from(request);
    }

    // REJECT REQUEST

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    @Transactional
    public MaintenanceRequestResponse rejectRequest(
            @PathVariable Long id,
            @RequestBody ReviewRequest body,
            Authentication authentication
    ) {
        if (body == null) {
            throw new IllegalArgumentException(
                    "Rejection remarks are required");
        }

        MaintenanceRequest request =
                maintenanceService.reviewRequest(
                        email(authentication),
                        id,
                        "REJECTED",
                        body.remarks()
                );

        return MaintenanceRequestResponse.from(request);
    }

    // MODULE STATUS

    @GetMapping("/requests/status-count")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN')")
    public Map<String, String> moduleStatus() {
        return Map.of(
                "module", "Maintenance Requests",
                "status", "ACTIVE"
        );
    }
}
