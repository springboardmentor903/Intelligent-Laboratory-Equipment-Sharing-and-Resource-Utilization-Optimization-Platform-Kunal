
package com.labresource.platform.maintenance.dto;

public record TechnicianTaskSummaryResponse(
        long totalTasks,
        long openTasks,
        long inProgressTasks,
        long overdueTasks,
        long completedTasks,
        long overdueGroup,
        long todayGroup,
        long thisWeekGroup,
        long laterGroup
) {
}
