
package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public class MaintenanceHistoryResponse {

    private Long workOrderId;
    private String workOrderNumber;

    private Long resourceId;
    private String resourceName;

    private WorkOrderType maintenanceType;

    private String technicianName;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;

    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private LocalDateTime completedAt;

    private long downtimeMinutes;

    private BigDecimal actualCost;
    private String resolutionNotes;

    public static MaintenanceHistoryResponse fromEntity(WorkOrder workOrder) {

        MaintenanceHistoryResponse response =
                new MaintenanceHistoryResponse();

        response.workOrderId = workOrder.getId();
        response.workOrderNumber = workOrder.getWorkOrderNumber();

        if (workOrder.getResource() != null) {
            response.resourceId = workOrder.getResource().getId();
            response.resourceName = workOrder.getResource().getName();
        }

        response.maintenanceType = workOrder.getType();

        if (workOrder.getAssignedTechnician() != null) {
            response.technicianName =
                    workOrder.getAssignedTechnician().getFullName();
        }

        response.scheduledStart = workOrder.getScheduledStart();
        response.scheduledEnd = workOrder.getScheduledEnd();

        response.actualStart = workOrder.getActualStart();
        response.actualEnd = workOrder.getActualEnd();
        response.completedAt = workOrder.getCompletedAt();

        if (workOrder.getActualStart() != null
                && workOrder.getActualEnd() != null) {

            response.downtimeMinutes = Math.max(
                    0,
                    Duration.between(
                            workOrder.getActualStart(),
                            workOrder.getActualEnd()
                    ).toMinutes()
            );
        }

        response.actualCost = workOrder.getActualCost();
        response.resolutionNotes = workOrder.getResolutionNotes();

        return response;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public WorkOrderType getMaintenanceType() {
        return maintenanceType;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public LocalDateTime getActualStart() {
        return actualStart;
    }

    public LocalDateTime getActualEnd() {
        return actualEnd;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public long getDowntimeMinutes() {
        return downtimeMinutes;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }
}
