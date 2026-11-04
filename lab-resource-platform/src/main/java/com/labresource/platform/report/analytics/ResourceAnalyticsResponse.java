package com.labresource.platform.report.analytics;

import java.time.LocalDate;
import java.util.List;

public record ResourceAnalyticsResponse(
        LocalDate fromDate,
        LocalDate toDate,
        double totalCapacityHours,
        double totalActualUsageHours,
        double totalIdleHours,
        double utilizationPercentage,
        List<ResourceIdleMetric> resources,
        List<PeakHourMetric> peakHours,
        List<HeatmapCell> heatmap
) {
    public record ResourceIdleMetric(
            Long resourceId,
            String resourceName,
            double capacityHours,
            double actualUsageHours,
            double idleHours,
            double utilizationPercentage
    ) {}

    public record PeakHourMetric(
            int hour,
            String label,
            double usageHours
    ) {}

    public record HeatmapCell(
            LocalDate date,
            int hour,
            double usageHours
    ) {}
}