
package com.labresource.platform.resource.dto;

import java.time.LocalDate;

public record CalibrationOverviewResponse(
        Long resourceId,
        String equipmentName,
        Long departmentId,
        String departmentName,
        LocalDate latestCalibrationDate,
        LocalDate nextDueDate,
        Long daysUntilDue,
        String calibrationState
) {
}
