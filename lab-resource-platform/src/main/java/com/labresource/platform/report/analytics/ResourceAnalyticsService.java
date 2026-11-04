
package com.labresource.platform.report.analytics;

import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResourceAnalyticsService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public ResourceAnalyticsService(
            EntityManager entityManager,
            UserRepository userRepository) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ResourceAnalyticsResponse getAnalytics(
            LocalDate fromDate,
            LocalDate toDate,
            String email) {

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // Future hours must not count as available capacity.
        LocalDateTime effectiveEnd =
                end.isBefore(now) ? end : now;

        if (effectiveEnd.isBefore(start)) {
            effectiveEnd = start;
        }

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        String role = user.getRole().getName()
                .replace("ROLE_", "");

        String scope;
        Long scopeId = null;

        switch (role) {
            case "SYSTEM_ADMIN":
                scope = "ALL";
                break;

            case "INSTITUTION_ADMIN":
                scope = "INSTITUTION";

                if (user.getInstitution() == null) {
                    throw new RuntimeException(
                            "Institution is not assigned to this user");
                }

                scopeId = user.getInstitution().getId();
                break;

            case "DEPARTMENT_HEAD":
            case "LAB_MANAGER":
                scope = "DEPARTMENT";

                if (user.getDepartment() == null) {
                    throw new RuntimeException(
                            "Department is not assigned to this user");
                }

                scopeId = user.getDepartment().getId();
                break;

            default:
                throw new RuntimeException(
                        "You are not authorized to view analytics");
        }

        // Fetch active resources within the user's scope.
        String resourceJpql =
                "SELECT r FROM Resource r WHERE r.isActive = true";

        if ("INSTITUTION".equals(scope)) {
            resourceJpql += " AND r.institution.id = :scopeId";
        } else if ("DEPARTMENT".equals(scope)) {
            resourceJpql += " AND r.department.id = :scopeId";
        }

        TypedQuery<Resource> resourceQuery =
                entityManager.createQuery(
                        resourceJpql, Resource.class);

        if (scopeId != null) {
            resourceQuery.setParameter("scopeId", scopeId);
        }

        List<Resource> resources = resourceQuery.getResultList();

        // Fetch actual usage only up to effectiveEnd.
        String usageJpql = """
                SELECT u FROM UsageLog u
                JOIN FETCH u.resource r
                WHERE u.actualStartTime < :end
                AND (u.actualEndTime IS NULL OR u.actualEndTime > :start)
                """;

        if ("INSTITUTION".equals(scope)) {
            usageJpql += " AND r.institution.id = :scopeId";
        } else if ("DEPARTMENT".equals(scope)) {
            usageJpql += " AND r.department.id = :scopeId";
        }

        TypedQuery<UsageLog> usageQuery =
                entityManager.createQuery(
                        usageJpql, UsageLog.class);

        usageQuery.setParameter("start", start);
        usageQuery.setParameter("end", effectiveEnd);

        if (scopeId != null) {
            usageQuery.setParameter("scopeId", scopeId);
        }

        List<UsageLog> logs = usageQuery.getResultList();

        Map<Long, Double> actualByResource = new HashMap<>();
        Map<Integer, Double> usageByHour = new TreeMap<>();
        Map<String, Double> heatmapData = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            usageByHour.put(hour, 0.0);
        }

        double totalActual = 0;

        for (UsageLog log : logs) {

            LocalDateTime usageStart =
                    log.getActualStartTime().isBefore(start)
                            ? start : log.getActualStartTime();

            LocalDateTime actualEnd =
                    log.getActualEndTime() == null
                            ? effectiveEnd
                            : log.getActualEndTime();

            LocalDateTime usageEnd =
                    actualEnd.isAfter(effectiveEnd)
                            ? effectiveEnd : actualEnd;

            if (!usageEnd.isAfter(usageStart)) {
                continue;
            }

            double duration =
                    Duration.between(
                            usageStart, usageEnd).toMillis()
                            / 3600000.0;

            Long resourceId = log.getResource().getId();

            actualByResource.merge(
                    resourceId, duration, Double::sum);

            totalActual += duration;

            LocalDateTime cursor = usageStart;

            while (cursor.isBefore(usageEnd)) {

                LocalDateTime nextHour =
                        cursor.withMinute(0)
                                .withSecond(0)
                                .withNano(0)
                                .plusHours(1);

                LocalDateTime segmentEnd =
                        nextHour.isBefore(usageEnd)
                                ? nextHour : usageEnd;

                double segmentHours =
                        Duration.between(
                                cursor, segmentEnd).toMillis()
                                / 3600000.0;

                int hour = cursor.getHour();

                usageByHour.merge(
                        hour, segmentHours, Double::sum);

                String key =
                        cursor.toLocalDate() + "_" + hour;

                heatmapData.merge(
                        key, segmentHours, Double::sum);

                cursor = segmentEnd;
            }
        }

        // Fetch approved/scheduled maintenance windows.
        String maintenanceJpql = """
                SELECT m
                FROM MaintenanceRequest m
                JOIN FETCH m.resource r
                WHERE m.status IN (
                    'SCHEDULED',
                    'IN_PROGRESS',
                    'COMPLETED'
                )
                AND m.scheduledStart IS NOT NULL
                AND m.scheduledEnd IS NOT NULL
                AND m.scheduledStart < :end
                AND m.scheduledEnd > :start
                """;

        if ("INSTITUTION".equals(scope)) {
            maintenanceJpql +=
                    " AND r.institution.id = :scopeId";
        } else if ("DEPARTMENT".equals(scope)) {
            maintenanceJpql +=
                    " AND r.department.id = :scopeId";
        }

        TypedQuery<MaintenanceRequest> maintenanceQuery =
                entityManager.createQuery(
                        maintenanceJpql,
                        MaintenanceRequest.class);

        maintenanceQuery.setParameter("start", start);
        maintenanceQuery.setParameter("end", effectiveEnd);

        if (scopeId != null) {
            maintenanceQuery.setParameter("scopeId", scopeId);
        }

        List<MaintenanceRequest> maintenanceRequests =
                maintenanceQuery.getResultList();

        // Group maintenance intervals by resource.
        Map<Long, List<MaintenanceInterval>> maintenanceByResource =
                new HashMap<>();

        for (MaintenanceRequest request : maintenanceRequests) {

            LocalDateTime maintenanceStart =
                    request.getScheduledStart().isBefore(start)
                            ? start : request.getScheduledStart();

            LocalDateTime maintenanceEnd =
                    request.getScheduledEnd().isAfter(effectiveEnd)
                            ? effectiveEnd : request.getScheduledEnd();

            if (!maintenanceEnd.isAfter(maintenanceStart)) {
                continue;
            }

            Long resourceId = request.getResource().getId();

            maintenanceByResource
                    .computeIfAbsent(
                            resourceId, id -> new ArrayList<>())
                    .add(new MaintenanceInterval(
                            maintenanceStart, maintenanceEnd));
        }

        // Calculate capacity, maintenance and idle hours.
        double totalCapacity = 0;
        double totalIdle = 0;

        List<ResourceAnalyticsResponse.ResourceIdleMetric>
                resourceMetrics = new ArrayList<>();

        double periodHours =
                Duration.between(start, effectiveEnd).toMillis()
                        / 3600000.0;

        for (Resource resource : resources) {

            double quantity = resource.getQuantity() == null
                    ? 0 : resource.getQuantity();

            double maintenanceHours =
                    calculateMergedMaintenanceHours(
                            maintenanceByResource.getOrDefault(
                                    resource.getId(),
                                    Collections.emptyList()));

            double availableHours =
                    Math.max(0, periodHours - maintenanceHours);

            double capacity = availableHours * quantity;

            double actual =
                    actualByResource.getOrDefault(
                            resource.getId(), 0.0);

            double idle =
                    Math.max(0, capacity - actual);

            double utilization = capacity > 0
                    ? (actual / capacity) * 100 : 0;

            totalCapacity += capacity;
            totalIdle += idle;

            resourceMetrics.add(
                    new ResourceAnalyticsResponse.ResourceIdleMetric(
                            resource.getId(),
                            resource.getName(),
                            round(capacity),
                            round(actual),
                            round(idle),
                            round(utilization)
                    )
            );
        }

        // Peak usage hours.
        List<ResourceAnalyticsResponse.PeakHourMetric>
                peakHours = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {

            String label = String.format("%02d:00", hour);

            peakHours.add(
                    new ResourceAnalyticsResponse.PeakHourMetric(
                            hour,
                            label,
                            round(usageByHour.get(hour))
                    )
            );
        }

        peakHours.sort(
                Comparator.comparingDouble(
                        ResourceAnalyticsResponse.PeakHourMetric
                                ::usageHours).reversed()
        );

        // 24-hour heatmap for every selected date.
        List<ResourceAnalyticsResponse.HeatmapCell>
                heatmap = new ArrayList<>();

        for (LocalDate date = fromDate;
             !date.isAfter(toDate);
             date = date.plusDays(1)) {

            for (int hour = 0; hour < 24; hour++) {

                String key = date + "_" + hour;

                heatmap.add(
                        new ResourceAnalyticsResponse.HeatmapCell(
                                date,
                                hour,
                                round(heatmapData.getOrDefault(
                                        key, 0.0))
                        )
                );
            }
        }

        double utilizationPercentage =
                totalCapacity > 0
                        ? (totalActual / totalCapacity) * 100
                        : 0;

        return new ResourceAnalyticsResponse(
                fromDate,
                toDate,
                round(totalCapacity),
                round(totalActual),
                round(totalIdle),
                round(utilizationPercentage),
                resourceMetrics,
                peakHours,
                heatmap
        );
    }

    // Merge overlapping maintenance intervals so they
    // are not deducted more than once.
    private double calculateMergedMaintenanceHours(
            List<MaintenanceInterval> intervals) {

        if (intervals == null || intervals.isEmpty()) {
            return 0;
        }

        List<MaintenanceInterval> sorted =
                new ArrayList<>(intervals);

        sorted.sort(
                Comparator.comparing(
                        MaintenanceInterval::start)
        );

        LocalDateTime currentStart = sorted.get(0).start();
        LocalDateTime currentEnd = sorted.get(0).end();

        double totalHours = 0;

        for (int i = 1; i < sorted.size(); i++) {

            MaintenanceInterval next = sorted.get(i);

            if (!next.start().isAfter(currentEnd)) {

                if (next.end().isAfter(currentEnd)) {
                    currentEnd = next.end();
                }

            } else {

                totalHours +=
                        Duration.between(
                                currentStart, currentEnd).toMillis()
                                / 3600000.0;

                currentStart = next.start();
                currentEnd = next.end();
            }
        }

        totalHours +=
                Duration.between(
                        currentStart, currentEnd).toMillis()
                        / 3600000.0;

        return totalHours;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record MaintenanceInterval(
            LocalDateTime start,
            LocalDateTime end) {
    }
}
