
package com.labresource.platform.maintenance.dto;

import java.time.LocalDate;

public record CalibrationCompletionResponse(
        Long workOrderId,
        String workOrderNumber,
        String workOrderStatus,
        Long calibrationId,
        LocalDate calibrationDate,
        LocalDate nextDueDate,
        String calibrationState
) {}
