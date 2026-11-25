package com.labresource.platform.dashboard.service;

import com.labresource.platform.dashboard.dto.ProcurementRecommendationResponse;
import com.labresource.platform.dashboard.dto.EquipmentLifecycleResponse;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.usage.entity.UsageCharge;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class InstitutionEquipmentAnalyticsService {

    private final EntityManager em;

    public InstitutionEquipmentAnalyticsService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public ProcurementRecommendationResponse getProcurementRecommendations(
            Long institutionId,
            BigDecimal highDemandThreshold,
            BigDecimal lowUtilizationThreshold,
            BigDecimal maintenanceThreshold
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from90 = now.minusDays(90);
        LocalDateTime from365 = now.minusDays(365);

        BigDecimal demandThreshold = highDemandThreshold == null
                ? new BigDecimal("80") : highDemandThreshold;
        BigDecimal lowThreshold = lowUtilizationThreshold == null
                ? new BigDecimal("20") : lowUtilizationThreshold;
        BigDecimal maintenanceRatio = maintenanceThreshold == null
                ? new BigDecimal("30") : maintenanceThreshold;

        List<Resource> resources = em.createQuery("""
                SELECT r FROM Resource r
                WHERE r.institution.id = :institutionId
                  AND r.isActive = true
                """, Resource.class)
                .setParameter("institutionId", institutionId)
                .getResultList();

        List<ProcurementRecommendationResponse.Recommendation> result =
                new ArrayList<>();

        Map<String, List<Resource>> categories = new HashMap<>();
        for (Resource resource : resources) {
            categories.computeIfAbsent(
                    resource.getCategory(), k -> new ArrayList<>()
            ).add(resource);
        }

        for (Map.Entry<String, List<Resource>> entry : categories.entrySet()) {
            List<Resource> categoryResources = entry.getValue();

            double totalCapacityHours = categoryResources.stream()
                    .mapToDouble(r -> 90.0 * 24.0
                            * Math.max(1, r.getQuantity() == null
                            ? 1 : r.getQuantity()))
                    .sum();

            double actualHours = getCategoryUsageHours(
                    institutionId, entry.getKey(), from90, now);

            BigDecimal categoryBookingRate = percentage(
                    actualHours, totalCapacityHours);

            long waitlistCount = em.createQuery("""
                    SELECT COUNT(w)
                    FROM BookingWaitlist w
                    WHERE w.resource.institution.id = :institutionId
                      AND w.resource.category = :category
                      AND w.createdAt >= :fromDate
                      AND w.status IN ('WAITING', 'OFFERED')
                    """, Long.class)
                    .setParameter("institutionId", institutionId)
                    .setParameter("category", entry.getKey())
                    .setParameter("fromDate", from90)
                    .getSingleResult();

            if (categoryBookingRate.compareTo(demandThreshold) >= 0
                    && waitlistCount >= 3) {
                for (Resource r : categoryResources) {
                    result.add(recommendation(
                            ProcurementRecommendationResponse.ReasonCode.HIGH_DEMAND,
                            r, categoryBookingRate, waitlistCount,
                            null, null
                    ));
                }
            }
        }

        for (Resource resource : resources) {
            double usageHours = getResourceUsageHours(
                    resource.getId(), from90, now);

            double capacity = 90.0 * 24.0
                    * Math.max(1, resource.getQuantity() == null
                    ? 1 : resource.getQuantity());

            BigDecimal utilization = percentage(usageHours, capacity);

            if (utilization.compareTo(lowThreshold) < 0) {
                result.add(recommendation(
                        ProcurementRecommendationResponse.ReasonCode.LOW_UTILIZATION,
                        resource, BigDecimal.ZERO, 0,
                        utilization, null
                ));
            }

            BigDecimal maintenanceCost = getMaintenanceCost(
                    resource.getId(), from365, now);

            BigDecimal purchaseCost = resource.getPurchaseCost();

            if (purchaseCost != null && purchaseCost.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = maintenanceCost
                        .multiply(BigDecimal.valueOf(100))
                        .divide(purchaseCost, 2, RoundingMode.HALF_UP);

                if (ratio.compareTo(maintenanceRatio) >= 0) {
                    result.add(recommendation(
                            ProcurementRecommendationResponse.ReasonCode.HIGH_MAINTENANCE,
                            resource, BigDecimal.ZERO, 0,
                            utilization, maintenanceCost
                    ));
                }
            }
        }

        return new ProcurementRecommendationResponse(
                now, institutionId, result
        );
    }

    @Transactional(readOnly = true)
    public EquipmentLifecycleResponse getLifecycle(
            Long institutionId,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination. Size must be between 1 and 100");
        }

        List<Resource> resources = em.createQuery("""
                SELECT r FROM Resource r
                WHERE r.institution.id = :institutionId
                ORDER BY r.name
                """, Resource.class)
                .setParameter("institutionId", institutionId)
                .getResultList();

        LocalDate today = LocalDate.now();

        List<EquipmentLifecycleResponse.EquipmentLifecycleItem> items =
                resources.stream()
                        .map(r -> lifecycleItem(r, today))
                        .toList();

        Page<EquipmentLifecycleResponse.EquipmentLifecycleItem> resultPage =
                new PageImpl<>(
                        items,
                        PageRequest.of(page, size),
                        items.size()
                );

        return new EquipmentLifecycleResponse(
                LocalDateTime.now(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.getContent()
        );
    }

    private EquipmentLifecycleResponse.EquipmentLifecycleItem lifecycleItem(
            Resource r, LocalDate today
    ) {
        Long id = r.getId();

        List<LocalDate> dueDates = em.createQuery("""
                SELECT c.nextDueDate
                FROM CalibrationRecord c
                WHERE c.resource.id = :resourceId
                ORDER BY c.calibrationDate DESC, c.id DESC
                """, LocalDate.class)
                .setParameter("resourceId", id)
                .setMaxResults(1)
                .getResultList();

        String calibrationState = "NONE";
        if (!dueDates.isEmpty()) {
            LocalDate due = dueDates.get(0);
            calibrationState = due.isBefore(today) ? "EXPIRED"
                    : due.isBefore(today.plusDays(31)) ? "DUE_SOON"
                      : "VALID";
        }

        Long workOrders = em.createQuery("""
                SELECT COUNT(w)
                FROM WorkOrder w
                WHERE w.resource.id = :resourceId
                """, Long.class)
                .setParameter("resourceId", id)
                .getSingleResult();

        List<Object[]> workOrderTimes = em.createQuery("""
                SELECT w.actualStart, w.actualEnd
                FROM WorkOrder w
                WHERE w.resource.id = :resourceId
                  AND w.actualStart IS NOT NULL
                """, Object[].class)
                .setParameter("resourceId", id)
                .getResultList();

        double downtime = 0;
        for (Object[] row : workOrderTimes) {
            LocalDateTime start = (LocalDateTime) row[0];
            LocalDateTime end = (LocalDateTime) row[1];
            if (end != null && end.isAfter(start)) {
                downtime += Duration.between(start, end).toSeconds() / 3600.0;
            }
        }

        BigDecimal maintenanceCost = getMaintenanceCost(
                id, LocalDateTime.now().minusYears(20), LocalDateTime.now());

        BigDecimal usageCharges = em.createQuery("""
                SELECT COALESCE(SUM(c.amount), 0)
                FROM UsageCharge c
                WHERE c.resource.id = :resourceId
                """, BigDecimal.class)
                .setParameter("resourceId", id)
                .getSingleResult();

        BigDecimal purchaseCost = r.getPurchaseCost();
        BigDecimal roi = null;

        if (purchaseCost != null && purchaseCost.compareTo(BigDecimal.ZERO) > 0) {
            roi = usageCharges.subtract(maintenanceCost)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(purchaseCost, 2, RoundingMode.HALF_UP);
        }

        Long ageDays = r.getPurchaseDate() == null
                ? null : Duration.between(
                r.getPurchaseDate().atStartOfDay(),
                today.atStartOfDay()).toDays();

        return new EquipmentLifecycleResponse.EquipmentLifecycleItem(
                id,
                r.getName(),
                r.getCategory(),
                r.getPurchaseDate(),
                ageDays,
                r.getStatus(),
                calibrationState,
                workOrders,
                decimal(downtime),
                purchaseCost,
                maintenanceCost,
                usageCharges,
                roi
        );
    }

    private ProcurementRecommendationResponse.Recommendation recommendation(
            ProcurementRecommendationResponse.ReasonCode reason,
            Resource r,
            BigDecimal bookingRate,
            long waitlist,
            BigDecimal utilization,
            BigDecimal maintenanceCost
    ) {
        BigDecimal purchaseCost = r.getPurchaseCost();
        BigDecimal ratio = BigDecimal.ZERO;

        if (purchaseCost != null && purchaseCost.compareTo(BigDecimal.ZERO) > 0
                && maintenanceCost != null) {
            ratio = maintenanceCost.multiply(BigDecimal.valueOf(100))
                    .divide(purchaseCost, 2, RoundingMode.HALF_UP);
        }

        return new ProcurementRecommendationResponse.Recommendation(
                reason,
                r.getId(),
                r.getName(),
                r.getCategory(),
                r.getDescription(),
                bookingRate,
                waitlist,
                utilization,
                maintenanceCost,
                purchaseCost,
                ratio
        );
    }

    private double getCategoryUsageHours(
            Long institutionId,
            String category,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Number value = em.createQuery("""
                SELECT COALESCE(SUM(
                    FUNCTION('timestampdiff', SECOND,
                        u.actualStartTime,
                        COALESCE(u.actualEndTime, :to)
                    )
                ), 0)
                FROM UsageLog u
                WHERE u.resource.institution.id = :institutionId
                  AND u.resource.category = :category
                  AND u.actualStartTime >= :from
                  AND u.actualStartTime < :to
                """, Number.class)
                .setParameter("institutionId", institutionId)
                .setParameter("category", category)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return value.doubleValue() / 3600.0;
    }

    private double getResourceUsageHours(
            Long resourceId, LocalDateTime from, LocalDateTime to
    ) {
        Number value = em.createQuery("""
                SELECT COALESCE(SUM(
                    FUNCTION('timestampdiff', SECOND,
                        u.actualStartTime,
                        COALESCE(u.actualEndTime, :to)
                    )
                ), 0)
                FROM UsageLog u
                WHERE u.resource.id = :resourceId
                  AND u.actualStartTime >= :from
                  AND u.actualStartTime < :to
                """, Number.class)
                .setParameter("resourceId", resourceId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return value.doubleValue() / 3600.0;
    }

    private BigDecimal getMaintenanceCost(
            Long resourceId, LocalDateTime from, LocalDateTime to
    ) {
        BigDecimal value = em.createQuery("""
                SELECT COALESCE(SUM(w.actualCost), 0)
                FROM WorkOrder w
                WHERE w.resource.id = :resourceId
                  AND w.createdAt >= :from
                  AND w.createdAt < :to
                """, BigDecimal.class)
                .setParameter("resourceId", resourceId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal percentage(double value, double total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(value * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}