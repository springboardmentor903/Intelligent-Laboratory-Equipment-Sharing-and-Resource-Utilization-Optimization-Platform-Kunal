
package com.labresource.platform.report.preview;

import com.labresource.platform.maintenance.dto.DowntimeAnalyticsResponse;
import com.labresource.platform.maintenance.service.DowntimeAnalyticsService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class MaintenanceReportBuilder {

    private static final int MAX_ROWS = 10_000;

    private final DowntimeAnalyticsService downtimeService;

    public MaintenanceReportBuilder(DowntimeAnalyticsService downtimeService) {
        this.downtimeService = downtimeService;
    }

    public ReportPreviewResponse build(
            LocalDate from, LocalDate to, String email
    ) {
        DowntimeAnalyticsResponse data =
                downtimeService.getInstitutionDowntimeAnalytics(
                        email,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()
                );

        List<Map<String, Object>> rows = new ArrayList<>();

        for (DowntimeAnalyticsResponse.EquipmentDowntimeSummary item
                : data.getEquipmentDetails()) {

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("resourceId", item.getResourceId());
            row.put("resourceName", item.getResourceName());
            row.put("downtimeMinutes", item.getDowntimeMinutes());
            row.put("openHoursDowntimeMinutes",
                    item.getDowntimeDuringOpenHoursMinutes());
            row.put("workOrderCount", item.getWorkOrderCount());
            rows.add(row);
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalDowntimeMinutes", data.getTotalDowntimeMinutes());
        totals.put("downtimeDuringOpenHoursMinutes",
                data.getDowntimeDuringOpenHoursMinutes());
        totals.put("totalOpenHoursMinutes", data.getTotalOpenHoursMinutes());
        totals.put("openHoursLostPercentage",
                data.getOpenHoursLostPercentage());
        totals.put("averageDowntimeMinutes",
                data.getAverageDowntimeMinutes());
        totals.put("workOrderCount", data.getWorkOrderCount());

        return new ReportPreviewResponse(
                "MAINTENANCE_DOWNTIME",
                "Maintenance and Downtime",
                from,
                to,
                List.of(
                        "resourceId", "resourceName",
                        "downtimeMinutes", "openHoursDowntimeMinutes",
                        "workOrderCount"
                ),
                rows.stream().limit(MAX_ROWS).toList(),
                totals,
                rows.size(),
                rows.size() > MAX_ROWS
        );
    }
}
