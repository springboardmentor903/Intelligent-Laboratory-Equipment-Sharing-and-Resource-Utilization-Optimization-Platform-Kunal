
package com.labresource.platform.dashboard.service;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.dashboard.dto.ResearcherDashboardResponse;
import com.labresource.platform.resource.entity.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ResearcherDashboardMapper {

    public ResearcherDashboardResponse.UpcomingBooking toUpcomingBooking(
            Booking booking
    ) {
        Resource resource = booking.getResource();

        return new ResearcherDashboardResponse.UpcomingBooking(
                booking.getId(),
                resource.getId(),
                resource.getName(),
                resource.getCategory(),
                resource.getLocation(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus()
        );
    }

    public ResearcherDashboardResponse.WaitlistOffer toWaitlistOffer(
            BookingWaitlist waitlist
    ) {
        Resource resource = waitlist.getResource();

        return new ResearcherDashboardResponse.WaitlistOffer(
                waitlist.getId(),
                resource.getId(),
                resource.getName(),
                waitlist.getOfferedAt(),
                null,
                waitlist.getStatus()
        );
    }

    public ResearcherDashboardResponse.DashboardNotification toNotification(
            Notification notification
    ) {
        return new ResearcherDashboardResponse.DashboardNotification(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt()
        );
    }

    public BigDecimal hours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public ResearcherDashboardResponse.UsageSummary usageSummary(
            LocalDateTime from,
            LocalDateTime to,
            Map<String, Long> statusCounts,
            long usageMinutes,
            long noShows,
            BigDecimal cost
    ) {
        return new ResearcherDashboardResponse.UsageSummary(
                from,
                to,
                statusCounts,
                hours(usageMinutes),
                noShows,
                cost == null ? BigDecimal.ZERO : cost
        );
    }

    public ResearcherDashboardResponse.WaitlistSummary waitlistSummary(
            long waiting,
            long offered,
            List<ResearcherDashboardResponse.WaitlistOffer> offers
    ) {
        return new ResearcherDashboardResponse.WaitlistSummary(
                waiting,
                offered,
                offers
        );
    }
}
