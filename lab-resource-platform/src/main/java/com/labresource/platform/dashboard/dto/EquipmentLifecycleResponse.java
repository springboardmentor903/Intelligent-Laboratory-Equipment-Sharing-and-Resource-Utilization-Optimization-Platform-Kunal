
package com.labresource.platform.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EquipmentLifecycleResponse(
        LocalDateTime generatedAt,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<EquipmentLifecycleItem> items
) {
    public record EquipmentLifecycleItem(
            Long equipmentId,
            String equipmentName,
            String category,
            LocalDate purchaseDate,
            Long ageInDays,
            String currentStatus,
            String latestCalibrationState,
            long workOrderCount,
            BigDecimal downtimeHours,
            BigDecimal purchaseCost,
            BigDecimal maintenanceCost,
            BigDecimal usageCharges,
            BigDecimal roiPercentage
    ) {}
}