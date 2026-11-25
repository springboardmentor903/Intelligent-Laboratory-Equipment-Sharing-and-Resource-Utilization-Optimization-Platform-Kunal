package com.labresource.platform.dashboard.controller;

import com.labresource.platform.dashboard.dto.ManagerDashboardResponse;
import com.labresource.platform.dashboard.service.ManagerDashboardService;
import com.labresource.platform.dashboard.service.ManagerDashboardSharingService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/dashboard")
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;
    private final ManagerDashboardSharingService sharingService;
    private final UserRepository userRepository;

    public ManagerDashboardController(
            ManagerDashboardService dashboardService,
            ManagerDashboardSharingService sharingService,
            UserRepository userRepository
    ) {
        this.dashboardService = dashboardService;
        this.sharingService = sharingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('LAB_MANAGER','DEPARTMENT_HEAD')")
    public ManagerDashboardResponse getManagerDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            Authentication authentication
    ) {
        String email = authentication.getName();

        ManagerDashboardResponse dashboard =
                dashboardService.getDashboard(email, fromDate, toDate);

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        String role = user.getRole() == null
                ? ""
                : user.getRole().getName().replace("ROLE_", "");

        List<ManagerDashboardResponse.DepartmentSharingRequest> requests =
                "DEPARTMENT_HEAD".equals(role)
                        && user.getDepartment() != null
                        ? sharingService.getDepartmentSharingRequests(
                        user.getDepartment().getId())
                        : List.of();

        return new ManagerDashboardResponse(
                dashboard.generatedAt(),
                dashboard.fromDate(),
                dashboard.toDate(),
                dashboard.scopeType(),
                dashboard.scopeId(),
                dashboard.utilization(),
                dashboard.utilizationHeatmap(),
                dashboard.bookingAdoptionPercentage(),
                dashboard.noShowRatePercentage(),
                dashboard.highDemandEquipment(),
                dashboard.openWorkOrdersByStatus(),
                dashboard.equipmentUnderMaintenance(),
                dashboard.preventiveSchedulesDueSoon(),
                dashboard.preventiveSchedulesOverdue(),
                dashboard.calibrationOverview(),
                dashboard.pendingBookingApprovals(),
                requests
        );
    }
}