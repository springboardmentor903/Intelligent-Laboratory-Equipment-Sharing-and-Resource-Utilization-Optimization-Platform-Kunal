
package com.labresource.platform.institution.controller;

import com.labresource.platform.institution.dto.UpdateInstitutionWorkingHoursRequest;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.institution.service.InstitutionWorkingHoursService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/institutions/working-hours")
@PreAuthorize("hasRole('LAB_MANAGER')")
public class InstitutionWorkingHoursController {

    private final InstitutionWorkingHoursService workingHoursService;

    public InstitutionWorkingHoursController(
            InstitutionWorkingHoursService workingHoursService) {
        this.workingHoursService = workingHoursService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWorkingHours(
            Principal principal) {

        Institution institution =
                workingHoursService.getWorkingHours(
                        principal.getName()
                );

        return ResponseEntity.ok(buildResponse(
                "Institution working hours retrieved successfully",
                institution
        ));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateWorkingHours(
            Principal principal,
            @Valid @RequestBody
            UpdateInstitutionWorkingHoursRequest request) {

        Institution institution =
                workingHoursService.updateWorkingHours(
                        principal.getName(),
                        request
                );

        return ResponseEntity.ok(buildResponse(
                "Institution working hours updated successfully",
                institution
        ));
    }

    private Map<String, Object> buildResponse(
            String message,
            Institution institution) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("message", message);
        response.put("institutionId", institution.getId());
        response.put("institutionName", institution.getName());
        response.put("openingTime", institution.getOpeningTime());
        response.put("closingTime", institution.getClosingTime());
        response.put("workingDays", institution.getWorkingDays());

        return response;
    }
}
