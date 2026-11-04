package com.labresource.platform.report;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ResourceUtilizationController {

    private final ResourceUtilizationService resourceUtilizationService;

    public ResourceUtilizationController(
            ResourceUtilizationService resourceUtilizationService) {

        this.resourceUtilizationService = resourceUtilizationService;
    }

    @GetMapping("/resource-utilization")
    @PreAuthorize("hasAnyRole('LAB_MANAGER','DEPARTMENT_HEAD','INSTITUTION_ADMIN','SYSTEM_ADMIN')")
    public List<ResourceUtilizationResponse> getResourceUtilization(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            Authentication authentication) {

        if ((fromDate == null) != (toDate == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both fromDate and toDate are required"
            );
        }

        if (fromDate == null) {
            fromDate = LocalDate.of(2000, 1, 1);
            toDate = LocalDate.now().plusDays(1);
        }

        if (toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "toDate cannot be before fromDate"
            );
        }

        return resourceUtilizationService.getResourceUtilization(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay(),
                authentication.getName()
        );
    }
}