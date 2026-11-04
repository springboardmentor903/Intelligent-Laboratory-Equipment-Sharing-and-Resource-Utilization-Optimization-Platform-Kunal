package com.labresource.platform.sharing.analytics;

import java.math.BigDecimal;
import java.util.List;

public record SharingAnalyticsResponse(
        long totalApprovedBookings,
        double totalHoursShared,
        BigDecimal totalFeesEarned,
        long partnerInstitutionCount,
        List<PartnerMetric> partners
) {
    public record PartnerMetric(
            Long institutionId,
            String institutionName,
            long approvedBookings,
            double hoursShared,
            BigDecimal feesEarned
    ) {}
}