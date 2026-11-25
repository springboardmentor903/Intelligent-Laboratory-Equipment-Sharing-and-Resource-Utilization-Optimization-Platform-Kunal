
package com.labresource.platform.maintenance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreatePreventiveScheduleRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotBlank(message = "Schedule title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Repeat interval is required")
    @Min(value = 1, message = "Repeat interval must be at least 1 day")
    private Integer repeatEveryDays;

    @NotNull(message = "Next due date is required")
    private LocalDateTime nextDueAt;

    public CreatePreventiveScheduleRequest() {
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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

    public LocalDateTime getNextDueAt() {
        return nextDueAt;
    }

    public void setNextDueAt(LocalDateTime nextDueAt) {
        this.nextDueAt = nextDueAt;
    }
}
