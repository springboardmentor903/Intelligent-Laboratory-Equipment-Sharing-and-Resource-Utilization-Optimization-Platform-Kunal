
package com.labresource.platform.maintenance.dto;

import java.util.ArrayList;
import java.util.List;

public class DowntimeAnalyticsResponse {

    private long totalDowntimeMinutes;
    private long downtimeDuringOpenHoursMinutes;
    private long totalOpenHoursMinutes;
    private double openHoursLostPercentage;
    private double averageDowntimeMinutes;
    private long workOrderCount;

    private List<EquipmentDowntimeSummary> equipmentDetails =
            new ArrayList<>();

    public long getTotalDowntimeMinutes() {
        return totalDowntimeMinutes;
    }

    public void setTotalDowntimeMinutes(long totalDowntimeMinutes) {
        this.totalDowntimeMinutes = totalDowntimeMinutes;
    }

    public long getDowntimeDuringOpenHoursMinutes() {
        return downtimeDuringOpenHoursMinutes;
    }

    public void setDowntimeDuringOpenHoursMinutes(
            long downtimeDuringOpenHoursMinutes) {
        this.downtimeDuringOpenHoursMinutes =
                downtimeDuringOpenHoursMinutes;
    }

    public long getTotalOpenHoursMinutes() {
        return totalOpenHoursMinutes;
    }

    public void setTotalOpenHoursMinutes(long totalOpenHoursMinutes) {
        this.totalOpenHoursMinutes = totalOpenHoursMinutes;
    }

    public double getOpenHoursLostPercentage() {
        return openHoursLostPercentage;
    }

    public void setOpenHoursLostPercentage(
            double openHoursLostPercentage) {
        this.openHoursLostPercentage = openHoursLostPercentage;
    }

    public double getAverageDowntimeMinutes() {
        return averageDowntimeMinutes;
    }

    public void setAverageDowntimeMinutes(
            double averageDowntimeMinutes) {
        this.averageDowntimeMinutes = averageDowntimeMinutes;
    }

    public long getWorkOrderCount() {
        return workOrderCount;
    }

    public void setWorkOrderCount(long workOrderCount) {
        this.workOrderCount = workOrderCount;
    }

    public List<EquipmentDowntimeSummary> getEquipmentDetails() {
        return equipmentDetails;
    }

    public void setEquipmentDetails(
            List<EquipmentDowntimeSummary> equipmentDetails) {
        this.equipmentDetails = equipmentDetails;
    }

    public static class EquipmentDowntimeSummary {

        private Long resourceId;
        private String resourceName;
        private long downtimeMinutes;
        private long downtimeDuringOpenHoursMinutes;
        private long workOrderCount;

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

        public long getDowntimeMinutes() {
            return downtimeMinutes;
        }

        public void setDowntimeMinutes(long downtimeMinutes) {
            this.downtimeMinutes = downtimeMinutes;
        }

        public long getDowntimeDuringOpenHoursMinutes() {
            return downtimeDuringOpenHoursMinutes;
        }

        public void setDowntimeDuringOpenHoursMinutes(
                long downtimeDuringOpenHoursMinutes) {
            this.downtimeDuringOpenHoursMinutes =
                    downtimeDuringOpenHoursMinutes;
        }

        public long getWorkOrderCount() {
            return workOrderCount;
        }

        public void setWorkOrderCount(long workOrderCount) {
            this.workOrderCount = workOrderCount;
        }
    }
}
