
package com.labresource.platform.sharing.controller;

import com.labresource.platform.sharing.dto.ExternalBookingResponse;
import com.labresource.platform.sharing.service.ExternalBookingService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/external-bookings")
@PreAuthorize("hasAnyRole('LAB_MANAGER', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN')")
public class ExternalBookingController {

    private final ExternalBookingService externalBookingService;

    public ExternalBookingController(
            ExternalBookingService externalBookingService) {
        this.externalBookingService = externalBookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalBookingResponse createRequest(
            @RequestBody ExternalBookingRequest request,
            Authentication authentication) {

        return externalBookingService.createRequest(
                request.shareId(),
                request.startTime(),
                request.endTime(),
                request.remarks(),
                authentication.getName()
        );
    }

    @GetMapping("/my")
    public List<ExternalBookingResponse> getMyRequests(
            Authentication authentication) {

        return externalBookingService.getMyRequests(
                authentication.getName()
        );
    }

    @GetMapping("/incoming")
    public List<ExternalBookingResponse> getIncomingRequests(
            Authentication authentication) {

        return externalBookingService.getIncomingRequests(
                authentication.getName()
        );
    }

    @GetMapping("/outgoing")
    public List<ExternalBookingResponse> getOutgoingRequests(
            Authentication authentication) {

        return externalBookingService.getOutgoingRequests(
                authentication.getName()
        );
    }

    @PutMapping("/{id}/approve")
    public ExternalBookingResponse approve(
            @PathVariable Long id,
            @RequestBody(required = false) DecisionRequest request,
            Authentication authentication) {

        return externalBookingService.approve(
                id,
                request == null ? null : request.remarks(),
                authentication.getName()
        );
    }

    @PutMapping("/{id}/reject")
    public ExternalBookingResponse reject(
            @PathVariable Long id,
            @RequestBody(required = false) DecisionRequest request,
            Authentication authentication) {

        return externalBookingService.reject(
                id,
                request == null ? null : request.remarks(),
                authentication.getName()
        );
    }

    @PutMapping("/{id}/cancel")
    public ExternalBookingResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {

        return externalBookingService.cancel(
                id,
                authentication.getName()
        );
    }

    public record ExternalBookingRequest(
            Long shareId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String remarks
    ) {
    }

    public record DecisionRequest(String remarks) {
    }
}
