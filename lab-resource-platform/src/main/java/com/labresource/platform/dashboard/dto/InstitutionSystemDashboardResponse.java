
package com.labresource.platform.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InstitutionSystemDashboardResponse(
        LocalDateTime generatedAt,
        SystemCounts systemCounts,
        long failedNotificationDeliveriesLast7Days,
        long pendingDepartmentHeadApprovals,
        long equipmentUnderMaintenance,
        LocalDateTime lastUtilizationSnapshotAt,
        List<PendingDepartmentHead> pendingApprovals,
        List<InstitutionUser> users
) {
    public record SystemCounts(
            long totalUsers,
            long activeUsers,
            long inactiveUsers,
            long totalEquipment,
            long activeEquipment,
            long totalDepartments
    ) {}

    public record PendingDepartmentHead(
            Long userId,
            String fullName,
            String email,
            Long departmentId,
            String departmentName,
            LocalDateTime requestedAt
    ) {}

    public record InstitutionUser(
            Long userId,
            String fullName,
            String email,
            String role,
            String status,
            Long departmentId,
            String departmentName,
            LocalDateTime createdAt,
            LocalDateTime lastLoginAt
    ) {}
}