
package com.labresource.platform.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ManagerDashboardResponse(
        LocalDateTime generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        String scopeType,
        Long scopeId,
        UtilizationSummary utilization,
        List<HeatmapCell> utilizationHeatmap,
        BigDecimal bookingAdoptionPercentage,
        BigDecimal noShowRatePercentage,
        List<EquipmentDemand> highDemandEquipment,
        Map<String, Long> openWorkOrdersByStatus,
        List<EquipmentMaintenance> equipmentUnderMaintenance,
        List<PreventiveSchedule> preventiveSchedulesDueSoon,
        List<PreventiveSchedule> preventiveSchedulesOverdue,
        CalibrationOverview calibrationOverview,
        List<BookingApproval> pendingBookingApprovals,
        List<DepartmentSharingRequest> departmentSharingRequests
) {
    public record UtilizationSummary(
            BigDecimal actualUtilizationPercentage,
            BigDecimal targetUtilizationPercentage,
            BigDecimal gapPercentage
    ) {}

    public record HeatmapCell(
            LocalDate date,
            int hour,
            BigDecimal usageHours
    ) {}

    public record EquipmentDemand(
            Long equipmentId,
            String equipmentName,
            String category,
            long bookingCount,
            long waitlistCount,
            BigDecimal utilizationPercentage
    ) {}

    public record EquipmentMaintenance(
            Long equipmentId,
            String equipmentName,
            String currentStatus,
            Long activeWorkOrderId,
            String workOrderStatus
    ) {}

    public record PreventiveSchedule(
            Long scheduleId,
            Long equipmentId,
            String equipmentName,
            LocalDate dueDate,
            String status
    ) {}

    public record CalibrationOverview(
            long validCount,
            long dueSoonCount,
            long expiredCount,
            long noneCount
    ) {}

    public record BookingApproval(
            Long bookingId,
            Long equipmentId,
            String equipmentName,
            String requesterName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status
    ) {}

    public record DepartmentSharingRequest(
            Long requestId,
            Long equipmentId,
            String equipmentName,
            String requestingDepartment,
            String targetInstitution,
            LocalDateTime requestedAt,
            String status
    ) {}
}