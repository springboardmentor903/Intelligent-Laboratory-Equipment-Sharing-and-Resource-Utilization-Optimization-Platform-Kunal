
package com.labresource.platform.sharing.controller;

import com.labresource.platform.sharing.entity.ResourceShare;
import com.labresource.platform.sharing.service.ResourceShareService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shares")
public class ResourceShareController {

    private final ResourceShareService shareService;

    public ResourceShareController(ResourceShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/discover")
    @PreAuthorize("isAuthenticated()")
    public List<ResourceShare> discoverSharedResources(
            Authentication authentication) {

        return shareService.discoverSharedResources(
                authentication.getName());
    }

    @GetMapping("/sent")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public List<ResourceShare> getSentRequests(
            Authentication authentication) {

        return shareService.getSentRequests(
                authentication.getName());
    }

    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public List<ResourceShare> getReceivedRequests(
            Authentication authentication) {

        return shareService.getReceivedRequests(
                authentication.getName());
    }

    // Create sharing request with hourly fee
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResourceShare createShare(
            @RequestBody ShareRequest request,
            Authentication authentication) {

        return shareService.createShare(
                request.resourceId(),
                request.targetInstitutionId(),
                request.startTime(),
                request.endTime(),
                request.feePerHour() == null
                        ? BigDecimal.ZERO
                        : request.feePerHour(),
                request.remarks(),
                authentication.getName()
        );
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ResourceShare> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) DecisionRequest request,
            Authentication authentication) {

        String remarks = request == null ? null : request.remarks();

        ResourceShare updated = shareService.approveRequest(
                id,
                remarks,
                authentication.getName()
        );

        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ResourceShare> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) DecisionRequest request,
            Authentication authentication) {

        String remarks = request == null ? null : request.remarks();

        ResourceShare updated = shareService.rejectRequest(
                id,
                remarks,
                authentication.getName()
        );

        return ResponseEntity.ok(updated);
    }

    public record ShareRequest(
            Long resourceId,
            Long targetInstitutionId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal feePerHour,
            String remarks
    ) {
    }

    public record DecisionRequest(String remarks) {
    }
}
