
package com.labresource.platform.maintenance.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MaintenanceStartPreviewResponse {

    private Long workOrderId;
    private String workOrderNumber;

    private Long resourceId;
    private String resourceName;

    private LocalDateTime maintenanceStart;
    private LocalDateTime expectedEnd;

    private int affectedBookingCount;
    private List<AffectedBooking> affectedBookings;

    public MaintenanceStartPreviewResponse() {
    }

    public MaintenanceStartPreviewResponse(
            Long workOrderId,
            String workOrderNumber,
            Long resourceId,
            String resourceName,
            LocalDateTime maintenanceStart,
            LocalDateTime expectedEnd,
            int affectedBookingCount,
            List<AffectedBooking> affectedBookings
    ) {
        this.workOrderId = workOrderId;
        this.workOrderNumber = workOrderNumber;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.maintenanceStart = maintenanceStart;
        this.expectedEnd = expectedEnd;
        this.affectedBookingCount = affectedBookingCount;
        this.affectedBookings = affectedBookings;
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

    public LocalDateTime getMaintenanceStart() {
        return maintenanceStart;
    }

    public void setMaintenanceStart(LocalDateTime maintenanceStart) {
        this.maintenanceStart = maintenanceStart;
    }

    public LocalDateTime getExpectedEnd() {
        return expectedEnd;
    }

    public void setExpectedEnd(LocalDateTime expectedEnd) {
        this.expectedEnd = expectedEnd;
    }

    public int getAffectedBookingCount() {
        return affectedBookingCount;
    }

    public void setAffectedBookingCount(int affectedBookingCount) {
        this.affectedBookingCount = affectedBookingCount;
    }

    public List<AffectedBooking> getAffectedBookings() {
        return affectedBookings;
    }

    public void setAffectedBookings(List<AffectedBooking> affectedBookings) {
        this.affectedBookings = affectedBookings;
    }

    public static class AffectedBooking {

        private Long bookingId;
        private String userName;
        private String userEmail;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String currentStatus;

        public AffectedBooking() {
        }

        public AffectedBooking(
                Long bookingId,
                String userName,
                String userEmail,
                LocalDateTime startTime,
                LocalDateTime endTime,
                String currentStatus
        ) {
            this.bookingId = bookingId;
            this.userName = userName;
            this.userEmail = userEmail;
            this.startTime = startTime;
            this.endTime = endTime;
            this.currentStatus = currentStatus;
        }

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public void setCurrentStatus(String currentStatus) {
            this.currentStatus = currentStatus;
        }
    }
}
