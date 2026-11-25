
package com.labresource.platform.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InstitutionDashboardResponse(
        LocalDateTime generatedAt,
        Long institutionId,
        String institutionName,
        List<DepartmentUtilization> departmentUtilization,
        List<MonthlyUtilization> monthlyUtilizationTrends,
        List<IdleEquipmentAlert> idleEquipmentAlerts,
        InterInstitutionSharing interInstitutionSharing,
        CrossDepartmentSharing crossDepartmentSharing,
        CostBudgetSummary costBudgetSummary,
        boolean readOnly
) {
    public record DepartmentUtilization(
            Long departmentId,
            String departmentName,
            BigDecimal utilizationPercentage,
            BigDecimal targetPercentage,
            BigDecimal gapPercentage
    ) {}

    public record MonthlyUtilization(
            int year,
            int month,
            BigDecimal utilizationPercentage,
            BigDecimal actualUsageHours,
            BigDecimal capacityHours
    ) {}

    public record IdleEquipmentAlert(
            Long equipmentId,
            String equipmentName,
            String category,
            LocalDate lastUsedDate,
            long idleDays,
            BigDecimal utilizationPercentage
    ) {}

    public record InterInstitutionSharing(
            long outgoingBookings,
            long incomingBookings,
            BigDecimal hoursShared,
            BigDecimal feesEarned,
            long partnerInstitutionCount
    ) {}

    public record CrossDepartmentSharing(
            BigDecimal totalHours,
            List<DepartmentPair> largestDepartmentPairs
    ) {}

    public record DepartmentPair(
            Long ownerDepartmentId,
            String ownerDepartmentName,
            Long userDepartmentId,
            String userDepartmentName,
            BigDecimal sharedHours
    ) {}

    public record CostBudgetSummary(
            BigDecimal totalUsageCharges,
            BigDecimal totalMaintenanceCost,
            BigDecimal allocatedBudget,
            BigDecimal consumedBudget,
            BigDecimal remainingBudget,
            BigDecimal budgetUtilizationPercentage
    ) {}
}