
package com.labresource.platform.usage.controller;

import com.labresource.platform.usage.service.CostOverviewService;
import com.labresource.platform.usage.dto.EquipmentCostResponse;
import com.labresource.platform.usage.dto.DepartmentAllocationResponse;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cost-overview")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','SYSTEM_ADMIN')")
public class CostOverviewController {

    private final CostOverviewService costOverviewService;
    private final UserRepository userRepository;

    public CostOverviewController(
            CostOverviewService costOverviewService,
            UserRepository userRepository) {
        this.costOverviewService = costOverviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/equipment")
    public List<EquipmentCostResponse> getEquipmentCosts(
            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false) Long departmentId) {

        LocalDate[] dates = resolveDates(fromDate, toDate);

        Long institutionId = getInstitutionId(authentication);

        return costOverviewService.getEquipmentCosts(
                institutionId,
                dates[0],
                dates[1],
                departmentId
        );
    }

    @GetMapping("/departments")
    public List<DepartmentAllocationResponse> getDepartmentAllocation(
            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false) Long departmentId) {

        LocalDate[] dates = resolveDates(fromDate, toDate);

        Long institutionId = getInstitutionId(authentication);

        return costOverviewService.getDepartmentAllocation(
                institutionId,
                dates[0],
                dates[1],
                departmentId
        );
    }

    private LocalDate[] resolveDates(
            LocalDate fromDate,
            LocalDate toDate) {

        LocalDate today = LocalDate.now();

        if (fromDate == null) {
            fromDate = today.minusDays(30);
        }

        if (toDate == null) {
            toDate = today;
        }

        if (fromDate.isAfter(toDate)
                || fromDate.isBefore(LocalDate.of(2000, 1, 1))
                || toDate.isAfter(today.plusDays(1))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range"
            );
        }

        return new LocalDate[]{fromDate, toDate};
    }

    private Long getInstitutionId(Authentication authentication) {

        User user = userRepository
                .findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not linked to an institution"
            );
        }

        return user.getInstitution().getId();
    }
}
