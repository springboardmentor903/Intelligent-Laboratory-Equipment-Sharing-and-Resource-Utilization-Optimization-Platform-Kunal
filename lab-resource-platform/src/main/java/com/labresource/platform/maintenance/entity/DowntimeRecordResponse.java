
package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public class DowntimeRecordResponse {

    private Long workOrderId;
    private String workOrderNumber;

    private Long resourceId;
    private String resourceName;

    private WorkOrderType maintenanceType;
    private WorkOrderStatus status;

    private String technicianName;

    private LocalDateTime downtimeStart;
    private LocalDateTime downtimeEnd;

    private long downtimeMinutes;

    private BigDecimal maintenanceCost;
    private String resolutionNotes;

    public static DowntimeRecordResponse fromEntity(WorkOrder workOrder) {

        DowntimeRecordResponse response =
                new DowntimeRecordResponse();

        response.setWorkOrderId(workOrder.getId());
        response.setWorkOrderNumber(workOrder.getWorkOrderNumber());

        if (workOrder.getResource() != null) {
            response.setResourceId(workOrder.getResource().getId());
            response.setResourceName(workOrder.getResource().getName());
        }

        response.setMaintenanceType(workOrder.getType());
        response.setStatus(workOrder.getStatus());

        if (workOrder.getAssignedTechnician() != null) {
            response.setTechnicianName(
                    workOrder.getAssignedTechnician().getFullName()
            );
        }

        response.setDowntimeStart(workOrder.getActualStart());
        response.setDowntimeEnd(workOrder.getActualEnd());

        if (workOrder.getActualStart() != null) {

            LocalDateTime endTime = workOrder.getActualEnd() != null
                    ? workOrder.getActualEnd()
                    : LocalDateTime.now();

            long minutes = Duration.between(
                    workOrder.getActualStart(),
                    endTime
            ).toMinutes();

            response.setDowntimeMinutes(Math.max(0, minutes));
        }

        response.setMaintenanceCost(workOrder.getActualCost());
        response.setResolutionNotes(workOrder.getResolutionNotes());

        return response;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public WorkOrderType getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(WorkOrderType maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public LocalDateTime getDowntimeStart() {
        return downtimeStart;
    }

    public void setDowntimeStart(LocalDateTime downtimeStart) {
        this.downtimeStart = downtimeStart;
    }

    public LocalDateTime getDowntimeEnd() {
        return downtimeEnd;
    }

    public void setDowntimeEnd(LocalDateTime downtimeEnd) {
        this.downtimeEnd = downtimeEnd;
    }

    public long getDowntimeMinutes() {
        return downtimeMinutes;
    }

    public void setDowntimeMinutes(long downtimeMinutes) {
        this.downtimeMinutes = downtimeMinutes;
    }

    public BigDecimal getMaintenanceCost() {
        return maintenanceCost;
    }

    public void setMaintenanceCost(BigDecimal maintenanceCost) {
        this.maintenanceCost = maintenanceCost;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
