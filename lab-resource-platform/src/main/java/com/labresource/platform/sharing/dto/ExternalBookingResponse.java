
package com.labresource.platform.sharing.dto;

import com.labresource.platform.sharing.entity.ExternalBooking;

import java.time.LocalDateTime;

public record ExternalBookingResponse(
        Long id,
        Long shareId,
        Long resourceId,
        String resourceName,
        Long resourceOwnerInstitutionId,
        Long requestingInstitutionId,
        Long requestedByUserId,
        String requestedByName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String remarks,
        String decisionRemarks,
        Long reviewedByUserId,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {

    public ExternalBookingResponse(ExternalBooking booking) {
        this(
                booking.getId(),
                booking.getResourceShare().getId(),
                booking.getResourceShare().getResource().getId(),
                booking.getResourceShare().getResource().getName(),
                booking.getResourceShare().getSharedByInstitutionId(),
                booking.getRequestingInstitutionId(),
                booking.getRequestedBy().getId(),
                booking.getRequestedBy().getFullName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getRemarks(),
                booking.getDecisionRemarks(),
                booking.getReviewedByUserId(),
                booking.getReviewedAt(),
                booking.getCreatedAt()
        );
    }
}
