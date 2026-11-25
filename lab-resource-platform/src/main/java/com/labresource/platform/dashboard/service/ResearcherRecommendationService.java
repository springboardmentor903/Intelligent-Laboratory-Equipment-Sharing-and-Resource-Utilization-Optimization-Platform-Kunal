package com.labresource.platform.dashboard.service;

import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.booking.repository.UsageLogRepository;
import com.labresource.platform.dashboard.dto.ResearcherDashboardResponse;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResearcherRecommendationService {

    private final BookingRepository bookingRepository;
    private final UsageLogRepository usageLogRepository;
    private final ResourceRepository resourceRepository;

    public ResearcherRecommendationService(
            BookingRepository bookingRepository,
            UsageLogRepository usageLogRepository,
            ResourceRepository resourceRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.usageLogRepository = usageLogRepository;
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<ResearcherDashboardResponse.RecommendedEquipment> getRecommendations(
            User user
    ) {
        if (user.getInstitution() == null) {
            return Collections.emptyList();
        }

        Long userId = user.getId();
        Long institutionId = user.getInstitution().getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime historyFrom = now.minusDays(180);
        LocalDateTime utilizationFrom = now.minusDays(90);

        List<Long> previouslyBooked =
                bookingRepository.findPreviouslyBookedResourceIds(userId);

        Set<Long> excludedIds = new HashSet<>(previouslyBooked);

        List<String> preferredCategories =
                bookingRepository.findUserTopBookedCategories(
                                userId, historyFrom
                        ).stream()
                        .map(row -> (String) row[0])
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        boolean hasHistory = !preferredCategories.isEmpty();

        if (!hasHistory) {
            List<Object[]> popularResources =
                    bookingRepository.findInstitutionPopularResources(
                            institutionId, utilizationFrom
                    );

            Set<Long> popularIds = new LinkedHashSet<>();

            for (Object[] row : popularResources) {
                popularIds.add(((Number) row[0]).longValue());
            }

            if (popularIds.isEmpty()) {
                return Collections.emptyList();
            }

            // Fallback: institution's most-booked equipment.
            List<Resource> candidates = resourceRepository.findAll()
                    .stream()
                    .filter(r -> r.getInstitution() != null)
                    .filter(r -> institutionId.equals(r.getInstitution().getId()))
                    .filter(r -> popularIds.contains(r.getId()))
                    .filter(r -> !excludedIds.contains(r.getId()))
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                    .filter(r -> r.getAvailableQuantity() != null
                            && r.getAvailableQuantity() > 0)
                    .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                    .toList();

            return rankCandidates(
                    candidates, institutionId, utilizationFrom, now,
                    "Popular equipment at your institution"
            );
        }

        List<Resource> candidates = resourceRepository
                .findRecommendationCandidates(
                        institutionId,
                        preferredCategories,
                        excludedIds.isEmpty()
                                ? List.of(-1L)
                                : new ArrayList<>(excludedIds)
                );

        return rankCandidates(
                candidates, institutionId, utilizationFrom, now,
                "Based on your previous booking categories"
        );
    }

    private List<ResearcherDashboardResponse.RecommendedEquipment> rankCandidates(
            List<Resource> candidates,
            Long institutionId,
            LocalDateTime fromDate,
            LocalDateTime now,
            String reason
    ) {
        Map<Long, Double> usageHours = new HashMap<>();

        for (Object[] row : usageLogRepository.findInstitutionRecentUsageHours(
                institutionId, fromDate, now
        )) {
            usageHours.put(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).doubleValue()
            );
        }

        return candidates.stream()
                .sorted(Comparator.comparingDouble(
                        r -> usageHours.getOrDefault(r.getId(), 0.0)
                ))
                .limit(5)
                .map(r -> {
                    double hours = usageHours.getOrDefault(r.getId(), 0.0);

                    // 90-day utilization proxy based on 24-hour equipment capacity.
                    int quantity = Math.max(
                            1,
                            r.getQuantity() == null ? 1 : r.getQuantity()
                    );

                    double capacityHours = 90.0 * 24.0 * quantity;

                    BigDecimal utilization = BigDecimal.valueOf(
                                    Math.min(100.0, hours * 100.0 / capacityHours)
                            )
                            .setScale(2, RoundingMode.HALF_UP);

                    return new ResearcherDashboardResponse.RecommendedEquipment(
                            r.getId(),
                            r.getName(),
                            r.getCategory(),
                            r.getDescription(),
                            r.getLocation(),
                            r.getStatus(),
                            r.getAvailableQuantity(),
                            utilization,
                            reason
                    );
                })
                .toList();
    }
}