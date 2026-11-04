package com.labresource.platform.report;

public class ResourceUtilizationResponse {

    private Long resourceId;
    private String resourceName;
    private long totalBookings;
    private long completedBookings;
    private double totalBookedHours;
    private double actualUsageHours;

    public ResourceUtilizationResponse(
            Long resourceId,
            String resourceName,
            long totalBookings,
            long completedBookings,
            double totalBookedHours,
            double actualUsageHours) {

        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.totalBookings = totalBookings;
        this.completedBookings = completedBookings;
        this.totalBookedHours = totalBookedHours;
        this.actualUsageHours = actualUsageHours;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public long getCompletedBookings() {
        return completedBookings;
    }

    public double getTotalBookedHours() {
        return totalBookedHours;
    }

    public double getActualUsageHours() {
        return actualUsageHours;
    }
}