
package com.labresource.platform.resource.dto;

import java.time.LocalDateTime;

public class LiveEquipmentStatusResponse {

    private Long resourceId;
    private String resourceName;
    private String category;
    private String location;
    private String status;

    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer activeSessionCount;

    private LocalDateTime currentSessionStart;
    private LocalDateTime scheduledBookingEnd;
    private Long elapsedMinutes;

    private LocalDateTime nextBookingStart;
    private LocalDateTime nextBookingEnd;

    public LiveEquipmentStatusResponse(
            Long resourceId,
            String resourceName,
            String category,
            String location,
            String status,
            Integer totalQuantity,
            Integer availableQuantity,
            Integer activeSessionCount,
            LocalDateTime currentSessionStart,
            LocalDateTime scheduledBookingEnd,
            Long elapsedMinutes,
            LocalDateTime nextBookingStart,
            LocalDateTime nextBookingEnd) {

        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.category = category;
        this.location = location;
        this.status = status;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.activeSessionCount = activeSessionCount;
        this.currentSessionStart = currentSessionStart;
        this.scheduledBookingEnd = scheduledBookingEnd;
        this.elapsedMinutes = elapsedMinutes;
        this.nextBookingStart = nextBookingStart;
        this.nextBookingEnd = nextBookingEnd;
    }

    public Long getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public Integer getActiveSessionCount() { return activeSessionCount; }
    public LocalDateTime getCurrentSessionStart() { return currentSessionStart; }
    public LocalDateTime getScheduledBookingEnd() { return scheduledBookingEnd; }
    public Long getElapsedMinutes() { return elapsedMinutes; }
    public LocalDateTime getNextBookingStart() { return nextBookingStart; }
    public LocalDateTime getNextBookingEnd() { return nextBookingEnd; }
}
