
package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.WorkOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WorkOrderResponse {

    private Long id;
    private String workOrderNumber;

    private Long resourceId;
    private String resourceName;

    private String type;
    private String status;

    private String title;
    private String description;

    private Long assignedTechnicianId;
    private String assignedTechnicianName;

    private Long createdById;
    private String createdByName;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private LocalDateTime expectedEnd;
    private LocalDateTime completedAt;

    private BigDecimal estimatedCost;
    private BigDecimal actualCost;

    private String resolutionNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkOrderResponse fromEntity(WorkOrder workOrder) {
        WorkOrderResponse response = new WorkOrderResponse();

        response.setId(workOrder.getId());
        response.setWorkOrderNumber(workOrder.getWorkOrderNumber());

        if (workOrder.getResource() != null) {
            response.setResourceId(workOrder.getResource().getId());
            response.setResourceName(workOrder.getResource().getName());
        }

        response.setType(
                workOrder.getType() == null
                        ? null : workOrder.getType().name()
        );

        response.setStatus(
                workOrder.getStatus() == null
                        ? null : workOrder.getStatus().name()
        );

        response.setTitle(workOrder.getTitle());
        response.setDescription(workOrder.getDescription());

        if (workOrder.getAssignedTechnician() != null) {
            response.setAssignedTechnicianId(
                    workOrder.getAssignedTechnician().getId()
            );
            response.setAssignedTechnicianName(
                    workOrder.getAssignedTechnician().getFullName()
            );
        }

        if (workOrder.getCreatedBy() != null) {
            response.setCreatedById(workOrder.getCreatedBy().getId());
            response.setCreatedByName(
                    workOrder.getCreatedBy().getFullName()
            );
        }

        response.setScheduledStart(workOrder.getScheduledStart());
        response.setScheduledEnd(workOrder.getScheduledEnd());
        response.setActualStart(workOrder.getActualStart());
        response.setActualEnd(workOrder.getActualEnd());
        response.setExpectedEnd(workOrder.getExpectedEnd());
        response.setCompletedAt(workOrder.getCompletedAt());

        response.setEstimatedCost(workOrder.getEstimatedCost());
        response.setActualCost(workOrder.getActualCost());
        response.setResolutionNotes(workOrder.getResolutionNotes());

        response.setCreatedAt(workOrder.getCreatedAt());
        response.setUpdatedAt(workOrder.getUpdatedAt());

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWorkOrderNumber() { return workOrderNumber; }
    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getAssignedTechnicianId() { return assignedTechnicianId; }
    public void setAssignedTechnicianId(Long assignedTechnicianId) { this.assignedTechnicianId = assignedTechnicianId; }

    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public void setAssignedTechnicianName(String assignedTechnicianName) { this.assignedTechnicianName = assignedTechnicianName; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime scheduledStart) { this.scheduledStart = scheduledStart; }

    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalDateTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }

    public LocalDateTime getActualStart() { return actualStart; }
    public void setActualStart(LocalDateTime actualStart) { this.actualStart = actualStart; }

    public LocalDateTime getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDateTime actualEnd) { this.actualEnd = actualEnd; }

    public LocalDateTime getExpectedEnd() { return expectedEnd; }
    public void setExpectedEnd(LocalDateTime expectedEnd) { this.expectedEnd = expectedEnd; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
