
package com.labresource.platform.booking.dto;

import com.labresource.platform.booking.entity.Booking;

import java.time.LocalDateTime;

public class BookingResponse {

    private Long id;
    private Long userId;
    private Long resourceId;
    private String resourceName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String statusRemark;

    public BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.userId = booking.getUser().getId();
        this.resourceId = booking.getResource().getId();
        this.resourceName = booking.getResource().getName();

        this.startTime = booking.getStartTime();
        this.endTime = booking.getEndTime();

        this.actualStartTime = booking.getActualStartTime();
        this.actualEndTime = booking.getActualEndTime();

        this.status = booking.getStatus();
        this.createdAt = booking.getCreatedAt();
        this.updatedAt = booking.getUpdatedAt();
        this.statusRemark = booking.getStatusRemark();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getStatusRemark() {
        return statusRemark;
    }
}
