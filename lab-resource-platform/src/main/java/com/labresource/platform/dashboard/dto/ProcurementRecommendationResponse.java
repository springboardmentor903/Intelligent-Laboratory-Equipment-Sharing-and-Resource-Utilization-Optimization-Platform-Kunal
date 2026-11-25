
package com.labresource.platform.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProcurementRecommendationResponse(
        LocalDateTime generatedAt,
        Long institutionId,
        List<Recommendation> recommendations
) {
    public enum ReasonCode {
        HIGH_DEMAND,
        LOW_UTILIZATION,
        HIGH_MAINTENANCE
    }

    public record Recommendation(
            ReasonCode reasonCode,
            Long equipmentId,
            String equipmentName,
            String category,
            String description,
            BigDecimal averageBookingRatePercentage,
            long waitlistEntriesLast90Days,
            BigDecimal utilizationLast90DaysPercentage,
            BigDecimal maintenanceCostLast12Months,
            BigDecimal purchaseCost,
            BigDecimal maintenanceToPurchaseCostPercentage
    ) {}
}