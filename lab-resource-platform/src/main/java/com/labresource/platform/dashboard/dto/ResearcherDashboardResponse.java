
package com.labresource.platform.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ResearcherDashboardResponse(
        LocalDateTime generatedAt,
        List<UpcomingBooking> upcomingBookings,
        Map<String, Long> equipmentAvailabilityByStatus,
        UsageSummary usageSummary,
        WaitlistSummary waitlist,
        NotificationSummary notifications,
        List<RecommendedEquipment> recommendations
) {
    public record UpcomingBooking(
            Long bookingId,
            Long equipmentId,
            String equipmentName,
            String category,
            String location,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status
    ) {}

    public record UsageSummary(
            LocalDateTime from,
            LocalDateTime to,
            Map<String, Long> bookingsByStatus,
            BigDecimal actualUsageHours,
            long noShowCount,
            BigDecimal usageCost
    ) {}

    public record WaitlistSummary(
            long waitingCount,
            long offeredCount,
            List<WaitlistOffer> offers
    ) {}

    public record WaitlistOffer(
            Long waitlistEntryId,
            Long equipmentId,
            String equipmentName,
            LocalDateTime offeredAt,
            LocalDateTime offerExpiresAt,
            String status
    ) {}

    public record NotificationSummary(
            long unreadCount,
            List<DashboardNotification> latest
    ) {}

    public record DashboardNotification(
            Long id,
            String title,
            String message,
            String type,
            boolean read,
            LocalDateTime createdAt
    ) {}

    public record RecommendedEquipment(
            Long equipmentId,
            String equipmentName,
            String category,
            String description,
            String location,
            String status,
            Integer availableQuantity,
            BigDecimal recentUtilizationPercentage,
            String recommendationReason
    ) {}
}