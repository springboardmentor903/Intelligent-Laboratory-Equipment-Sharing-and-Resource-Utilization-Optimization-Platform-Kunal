
package com.labresource.platform.dashboard.service;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.booking.repository.UsageLogRepository;
import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import com.labresource.platform.booking.waitlist.repository.BookingWaitlistRepository;
import com.labresource.platform.dashboard.dto.ResearcherDashboardResponse;
import com.labresource.platform.dashboard.repository.ResearcherDashboardRepository;
import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.notification.repository.NotificationRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.usage.repository.UsageChargeRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResearcherDashboardService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UsageLogRepository usageLogRepository;
    private final BookingWaitlistRepository waitlistRepository;
    private final NotificationRepository notificationRepository;
    private final ResourceRepository resourceRepository;
    private final UsageChargeRepository usageChargeRepository;
    private final ResearcherDashboardRepository dashboardRepository;
    private final ResearcherDashboardMapper mapper;

    public ResearcherDashboardService(
            UserRepository userRepository,
            BookingRepository bookingRepository,
            UsageLogRepository usageLogRepository,
            BookingWaitlistRepository waitlistRepository,
            NotificationRepository notificationRepository,
            ResourceRepository resourceRepository,
            UsageChargeRepository usageChargeRepository,
            ResearcherDashboardRepository dashboardRepository,
            ResearcherDashboardMapper mapper
    ) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.usageLogRepository = usageLogRepository;
        this.waitlistRepository = waitlistRepository;
        this.notificationRepository = notificationRepository;
        this.resourceRepository = resourceRepository;
        this.usageChargeRepository = usageChargeRepository;
        this.dashboardRepository = dashboardRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ResearcherDashboardResponse getDashboard(String email) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        if (user.getInstitution() == null) {
            throw new IllegalStateException(
                    "User is not associated with an institution");
        }

        Long userId = user.getId();
        Long institutionId = user.getInstitution().getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime usageFrom = now.minusDays(90);
        LocalDateTime recommendationFrom = now.minusDays(180);

        List<Booking> allUserBookings =
                bookingRepository.findByUserIdWithDetails(userId);

        List<ResearcherDashboardResponse.UpcomingBooking> upcoming =
                allUserBookings.stream()
                        .filter(b -> b.getStartTime() != null)
                        .filter(b -> !b.getStartTime().isBefore(now))
                        .filter(b -> List.of(
                                "PENDING", "APPROVED",
                                "CONFIRMED", "IN_USE"
                        ).contains(b.getStatus()))
                        .sorted(Comparator.comparing(Booking::getStartTime))
                        .limit(5)
                        .map(mapper::toUpcomingBooking)
                        .toList();

        Map<String, Long> availability = resourceRepository.findAll()
                .stream()
                .filter(r -> r.getInstitution() != null)
                .filter(r -> Objects.equals(
                        r.getInstitution().getId(), institutionId))
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .collect(Collectors.groupingBy(
                        r -> r.getStatus() == null
                                ? "UNKNOWN" : r.getStatus(),
                        Collectors.counting()
                ));

        Map<String, Long> statusCounts = new LinkedHashMap<>();

        dashboardRepository.countUserBookingsByStatus(
                        userId, usageFrom, now)
                .forEach(row -> statusCounts.put(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()
                ));

        long usageMinutes = 0;

        for (UsageLog log :
                usageLogRepository.findByUserIdOrderByActualStartTimeDesc(userId)) {

            LocalDateTime start = log.getActualStartTime();

            if (start == null || start.isBefore(usageFrom)
                    || start.isAfter(now)) {
                continue;
            }

            LocalDateTime end = log.getActualEndTime() == null
                    ? now : log.getActualEndTime();

            if (end.isAfter(now)) {
                end = now;
            }

            if (end.isAfter(start)) {
                usageMinutes += Duration.between(start, end).toMinutes();
            }
        }

        long noShows = allUserBookings.stream()
                .filter(b -> b.getCreatedAt() != null)
                .filter(b -> !b.getCreatedAt().isBefore(usageFrom))
                .filter(b -> "NO_SHOW".equalsIgnoreCase(b.getStatus()))
                .count();

        BigDecimal usageCost = usageChargeRepository.sumUserCharges(
                userId,
                usageFrom.atZone(ZoneId.systemDefault()).toInstant(),
                now.atZone(ZoneId.systemDefault()).toInstant(),
                null,
                null
        );

        ResearcherDashboardResponse.UsageSummary usageSummary =
                mapper.usageSummary(
                        usageFrom,
                        now,
                        statusCounts,
                        usageMinutes,
                        noShows,
                        usageCost
                );

        List<BookingWaitlist> waitlistEntries =
                waitlistRepository.findByUserEmailOrderByCreatedAtDesc(email);

        long waitingCount = waitlistEntries.stream()
                .filter(w -> "WAITING".equalsIgnoreCase(w.getStatus()))
                .count();

        List<BookingWaitlist> offeredEntries = waitlistEntries.stream()
                .filter(w -> "OFFERED".equalsIgnoreCase(w.getStatus()))
                .toList();

        List<ResearcherDashboardResponse.WaitlistOffer> offers =
                offeredEntries.stream()
                        .limit(5)
                        .map(mapper::toWaitlistOffer)
                        .toList();

        ResearcherDashboardResponse.WaitlistSummary waitlistSummary =
                mapper.waitlistSummary(
                        waitingCount,
                        offeredEntries.size(),
                        offers
                );

        long unreadCount =
                notificationRepository.countByUserIdAndIsRead(userId, false);

        List<ResearcherDashboardResponse.DashboardNotification> latest =
                notificationRepository
                        .findTop5ByUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .limit(3)
                        .map(mapper::toNotification)
                        .toList();

        ResearcherDashboardResponse.NotificationSummary notificationSummary =
                new ResearcherDashboardResponse.NotificationSummary(
                        unreadCount, latest
                );

        Set<Long> previouslyBookedResourceIds = allUserBookings.stream()
                .filter(b -> b.getResource() != null)
                .map(b -> b.getResource().getId())
                .collect(Collectors.toSet());

        List<Object[]> categoryRows =
                dashboardRepository.findUserTopCategories(
                        userId, recommendationFrom, now);

        Set<String> preferredCategories = categoryRows.stream()
                .limit(3)
                .map(row -> String.valueOf(row[0]))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Object[]> bookingRows =
                dashboardRepository.findInstitutionResourceBookingCounts(
                        institutionId, usageFrom, now);

        Map<Long, Long> bookingCounts = new HashMap<>();

        bookingRows.forEach(row -> bookingCounts.put(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue()
        ));

        List<Object[]> usageRows =
                dashboardRepository.findInstitutionResourceUsageHours(
                        institutionId, usageFrom, now);

        Map<Long, BigDecimal> usageHours = new HashMap<>();

        usageRows.forEach(row -> usageHours.put(
                ((Number) row[0]).longValue(),
                new BigDecimal(row[1].toString())
        ));

        List<Resource> candidates = resourceRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .filter(r -> r.getInstitution() != null
                        && Objects.equals(
                        r.getInstitution().getId(), institutionId))
                .filter(r -> r.getAvailableQuantity() != null
                        && r.getAvailableQuantity() > 0)
                .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                .filter(r -> !previouslyBookedResourceIds.contains(r.getId()))
                .filter(r -> preferredCategories.isEmpty()
                        || preferredCategories.contains(r.getCategory()))
                .sorted(Comparator
                        .comparingLong((Resource r) ->
                                bookingCounts.getOrDefault(r.getId(), 0L))
                        .thenComparing(Resource::getName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(5)
                .toList();

        List<ResearcherDashboardResponse.RecommendedEquipment> recommendations =
                candidates.stream()
                        .map(r -> {
                            BigDecimal actualHours = usageHours.getOrDefault(
                                    r.getId(), BigDecimal.ZERO);

                            int quantity = r.getQuantity() == null
                                    ? 1 : Math.max(1, r.getQuantity());

                            BigDecimal capacityHours =
                                    BigDecimal.valueOf(quantity)
                                            .multiply(BigDecimal.valueOf(90L * 24L));

                            BigDecimal utilization = actualHours
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(capacityHours, 2, RoundingMode.HALF_UP);

                            if (utilization.compareTo(BigDecimal.valueOf(100)) > 0) {
                                utilization = BigDecimal.valueOf(100);
                            }

                            return new ResearcherDashboardResponse.RecommendedEquipment(
                                    r.getId(),
                                    r.getName(),
                                    r.getCategory(),
                                    r.getDescription(),
                                    r.getLocation(),
                                    r.getStatus(),
                                    r.getAvailableQuantity(),
                                    utilization,
                                    preferredCategories.isEmpty()
                                            ? "Popular equipment in your institution"
                                            : "Based on your booking category history"
                            );
                        })
                        .toList();

        return new ResearcherDashboardResponse(
                now,
                upcoming,
                availability,
                usageSummary,
                waitlistSummary,
                notificationSummary,
                recommendations
        );
    }
}
