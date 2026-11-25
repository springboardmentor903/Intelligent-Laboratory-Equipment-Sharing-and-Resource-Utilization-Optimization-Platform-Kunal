
package com.labresource.platform.maintenance.dto;

import java.time.LocalDateTime;

public record TechnicianTaskResponse(
        Long workOrderId,
        String workOrderNumber,
        String title,
        String description,
        String workOrderType,
        String status,
        String equipmentName,
        Long resourceId,
        String departmentName,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        LocalDateTime expectedEnd,
        LocalDateTime actualStart,
        String priority,
        String taskGroup,
        boolean overdue
) {
}
