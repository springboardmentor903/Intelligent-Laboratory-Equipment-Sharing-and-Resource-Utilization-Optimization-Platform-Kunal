
package com.labresource.platform.maintenance.dto;

import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import java.time.LocalDateTime;

public record MaintenanceRequestResponse(
        Long id,
        Long resourceId,
        String resourceName,
        Long requesterId,
        String requesterName,
        String title,
        String description,
        String priority,
        String status,
        Long reviewedById,
        String reviewedByName,
        String reviewRemarks,
        LocalDateTime reviewedAt,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        LocalDateTime completedAt,
        String resolutionNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MaintenanceRequestResponse from(MaintenanceRequest request) {
        if (request == null) {
            return null;
        }

        Long resourceId = null;
        String resourceName = null;

        if (request.getResource() != null) {
            resourceId = request.getResource().getId();
            resourceName = request.getResource().getName();
        }

        Long requesterId = null;
        String requesterName = null;

        if (request.getRequester() != null) {
            requesterId = request.getRequester().getId();
            requesterName = request.getRequester().getFullName();
        }

        Long reviewedById = null;
        String reviewedByName = null;

        if (request.getReviewedBy() != null) {
            reviewedById = request.getReviewedBy().getId();
            reviewedByName = request.getReviewedBy().getFullName();
        }

        return new MaintenanceRequestResponse(
                request.getId(),
                resourceId,
                resourceName,
                requesterId,
                requesterName,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getStatus(),
                reviewedById,
                reviewedByName,
                request.getReviewRemarks(),
                request.getReviewedAt(),
                request.getScheduledStart(),
                request.getScheduledEnd(),
                request.getCompletedAt(),
                request.getResolutionNotes(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
