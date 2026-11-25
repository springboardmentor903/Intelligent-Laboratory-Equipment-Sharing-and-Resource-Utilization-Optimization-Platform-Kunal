package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.WorkOrderServiceLog;

import java.time.LocalDateTime;

public record WorkOrderServiceLogResponse(
        Long id,
        String oldStatus,
        String newStatus,
        UserSummary changedBy,
        String remarks,
        LocalDateTime changedAt
) {

    public static WorkOrderServiceLogResponse fromEntity(
            WorkOrderServiceLog log
    ) {

        UserSummary changedBy = null;

        if (log.getChangedBy() != null) {
            changedBy = new UserSummary(
                    log.getChangedBy().getId(),
                    log.getChangedBy().getFullName(),
                    log.getChangedBy().getEmail()
            );
        }

        return new WorkOrderServiceLogResponse(
                log.getId(),
                log.getOldStatus(),
                log.getNewStatus(),
                changedBy,
                log.getRemarks(),
                log.getChangedAt()
        );
    }

    public record UserSummary(
            Long id,
            String fullName,
            String email
    ) {
    }
}