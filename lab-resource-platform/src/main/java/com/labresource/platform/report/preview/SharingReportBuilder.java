
package com.labresource.platform.report.preview;

import com.labresource.platform.sharing.analytics.SharingAnalyticsResponse;
import com.labresource.platform.sharing.analytics.SharingAnalyticsService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class SharingReportBuilder {

    private static final int MAX_ROWS = 10_000;

    private final SharingAnalyticsService sharingService;

    public SharingReportBuilder(SharingAnalyticsService sharingService) {
        this.sharingService = sharingService;
    }

    public ReportPreviewResponse build(
            LocalDate from, LocalDate to, String email
    ) {
        SharingAnalyticsResponse data =
                sharingService.getAnalytics(from, to, email);

        List<Map<String, Object>> rows = new ArrayList<>();

        for (SharingAnalyticsResponse.PartnerMetric item : data.partners()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("institutionId", item.institutionId());
            row.put("institutionName", item.institutionName());
            row.put("approvedBookings", item.approvedBookings());
            row.put("hoursShared", item.hoursShared());
            row.put("feesEarned", item.feesEarned());
            rows.add(row);
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalApprovedBookings", data.totalApprovedBookings());
        totals.put("totalHoursShared", data.totalHoursShared());
        totals.put("totalFeesEarned", data.totalFeesEarned());
        totals.put("partnerInstitutionCount", data.partnerInstitutionCount());

        return new ReportPreviewResponse(
                "INTER_INSTITUTION_SHARING",
                "Inter-Institution Sharing",
                from,
                to,
                List.of(
                        "institutionId", "institutionName",
                        "approvedBookings", "hoursShared", "feesEarned"
                ),
                rows.stream().limit(MAX_ROWS).toList(),
                totals,
                rows.size(),
                rows.size() > MAX_ROWS
        );
    }
}
