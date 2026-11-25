
package com.labresource.platform.resource.dto;

import java.time.LocalDate;

public record CalibrationReminderResponse(
        Long calibrationId,
        Long resourceId,
        String equipmentName,
        LocalDate calibrationDate,
        LocalDate nextDueDate,
        Long daysUntilDue,
        String reminderStatus
) {}
