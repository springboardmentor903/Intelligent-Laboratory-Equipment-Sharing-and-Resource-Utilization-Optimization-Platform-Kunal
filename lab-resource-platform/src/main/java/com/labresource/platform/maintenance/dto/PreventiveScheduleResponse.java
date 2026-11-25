
package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.PreventiveMaintenanceSchedule;

import java.time.LocalDateTime;

public class PreventiveScheduleResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private String title;
    private String description;
    private Integer repeatEveryDays;
    private LocalDateTime lastCompletedAt;
    private LocalDateTime nextDueAt;
    private Boolean active;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PreventiveScheduleResponse() {
    }

    public static PreventiveScheduleResponse fromEntity(
            PreventiveMaintenanceSchedule schedule) {

        PreventiveScheduleResponse response =
                new PreventiveScheduleResponse();

        response.setId(schedule.getId());
        response.setResourceId(schedule.getResource().getId());
        response.setResourceName(schedule.getResource().getName());
        response.setTitle(schedule.getTitle());
        response.setDescription(schedule.getDescription());
        response.setRepeatEveryDays(schedule.getRepeatEveryDays());
        response.setLastCompletedAt(schedule.getLastCompletedAt());
        response.setNextDueAt(schedule.getNextDueAt());
        response.setActive(schedule.getActive());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        if (!Boolean.TRUE.equals(schedule.getActive())) {
            response.setStatus("INACTIVE");
        } else if (schedule.getNextDueAt() == null) {
            response.setStatus("UNKNOWN");
        } else if (schedule.getNextDueAt().isBefore(LocalDateTime.now())) {
            response.setStatus("OVERDUE");
        } else if (!schedule.getNextDueAt().isAfter(
                LocalDateTime.now().plusDays(7))) {
            response.setStatus("DUE_SOON");
        } else {
            response.setStatus("OK");
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRepeatEveryDays() {
        return repeatEveryDays;
    }

    public void setRepeatEveryDays(Integer repeatEveryDays) {
        this.repeatEveryDays = repeatEveryDays;
    }

    public LocalDateTime getLastCompletedAt() {
        return lastCompletedAt;
    }

    public void setLastCompletedAt(LocalDateTime lastCompletedAt) {
        this.lastCompletedAt = lastCompletedAt;
    }

    public LocalDateTime getNextDueAt() {
        return nextDueAt;
    }

    public void setNextDueAt(LocalDateTime nextDueAt) {
        this.nextDueAt = nextDueAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
