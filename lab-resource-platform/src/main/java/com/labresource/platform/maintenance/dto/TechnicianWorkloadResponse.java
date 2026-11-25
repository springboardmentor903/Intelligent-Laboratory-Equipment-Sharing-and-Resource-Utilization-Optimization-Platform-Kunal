
package com.labresource.platform.maintenance.dto;

import java.time.LocalDateTime;

public record TechnicianWorkloadResponse(
        Long technicianId,
        String technicianName,
        String email,
        long activeTasks,
        long inProgressTasks,
        long overdueTasks,
        LocalDateTime nextScheduledDate
) {
}
