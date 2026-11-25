package com.labresource.platform.dashboard.service;

import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.dashboard.dto.ManagerDashboardResponse;
import com.labresource.platform.maintenance.entity.PreventiveMaintenanceSchedule;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ManagerDashboardService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public ManagerDashboardService(
            EntityManager entityManager,
            UserRepository userRepository
    ) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard(
            String email,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate today = LocalDate.now();

        if (fromDate == null) {
            fromDate = today.minusDays(29);
        }
        if (toDate == null) {
            toDate = today;
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException(
                    "To date cannot be before from date");
        }
        if (fromDate.isBefore(today.minusYears(2))) {
            throw new IllegalArgumentException(
                    "Dashboard date range cannot exceed two years");
        }

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        String role = user.getRole().getName().replace("ROLE_", "");

        Long institutionId;
        Long departmentId = null;
        String scopeType;

        if ("LAB_MANAGER".equals(role)) {
            if (user.getInstitution() == null) {
                throw new IllegalStateException(
                        "Institution is not assigned to this Lab Manager");
            }
            institutionId = user.getInstitution().getId();
            scopeType = "INSTITUTION";
        } else if ("DEPARTMENT_HEAD".equals(role)) {
            if (user.getInstitution() == null || user.getDepartment() == null) {
                throw new IllegalStateException(
                        "Institution or department is not assigned");
            }
            institutionId = user.getInstitution().getId();
            departmentId = user.getDepartment().getId();
            scopeType = "DEPARTMENT";
        } else {
            throw new SecurityException(
                    "Only Lab Managers and Department Heads can access this dashboard");
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();
        LocalDateTime effectiveEnd = end.isBefore(LocalDateTime.now())
                ? end : LocalDateTime.now();

        List<Resource> resources = fetchResources(institutionId, departmentId);

        Map<Long, Double> usageByResource = new HashMap<>();
        Map<String, Double> heatmapValues = new HashMap<>();
        double totalActualHours = calculateUsage(
                institutionId, departmentId, start, effectiveEnd,
                usageByResource, heatmapValues);

        double capacityHours = resources.stream()
                .mapToDouble(r -> {
                    int quantity = r.getQuantity() == null
                            ? 1 : Math.max(1, r.getQuantity());
                    return Duration.between(start, effectiveEnd).toMinutes()
                            / 60.0 * quantity;
                })
                .sum();

        BigDecimal utilization = percentage(
                totalActualHours, capacityHours);

        BigDecimal target = BigDecimal.valueOf(70);
        BigDecimal gap = utilization.subtract(target)
                .setScale(2, RoundingMode.HALF_UP);

        List<ManagerDashboardResponse.HeatmapCell> heatmap =
                buildHeatmap(heatmapValues);

        long totalBookings = countBookings(
                institutionId, departmentId, start, end, null);

        long noShows = countBookings(
                institutionId, departmentId, start, end, "NO_SHOW");

        long bookableResources = resources.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .filter(r -> r.getAvailableQuantity() != null
                        && r.getAvailableQuantity() > 0)
                .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getStatus()))
                .count();

        long adoptedResources = countAdoptedResources(
                institutionId, departmentId, start, end);

        BigDecimal adoption = percentage(
                adoptedResources, bookableResources);

        BigDecimal noShowRate = percentage(noShows, totalBookings);

        List<ManagerDashboardResponse.EquipmentDemand> demand =
                buildHighDemand(
                        resources, institutionId, departmentId,
                        start, end, usageByResource);

        Map<String, Long> workOrderCounts =
                getWorkOrderCounts(institutionId, departmentId);

        List<ManagerDashboardResponse.EquipmentMaintenance> maintenance =
                getActiveMaintenance(institutionId, departmentId);

        List<ManagerDashboardResponse.PreventiveSchedule> dueSoon =
                getSchedules(
                        institutionId, departmentId,
                        today, today.plusDays(30), false);

        List<ManagerDashboardResponse.PreventiveSchedule> overdue =
                getSchedules(
                        institutionId, departmentId,
                        null, today, true);

        ManagerDashboardResponse.CalibrationOverview calibration =
                getCalibrationOverview(
                        institutionId, departmentId, today);

        List<ManagerDashboardResponse.BookingApproval> approvals =
                "LAB_MANAGER".equals(role)
                        ? getPendingApprovals(institutionId, departmentId)
                        : Collections.emptyList();

        return new ManagerDashboardResponse(
                LocalDateTime.now(),
                fromDate,
                toDate,
                scopeType,
                departmentId == null ? institutionId : departmentId,
                new ManagerDashboardResponse.UtilizationSummary(
                        utilization, target, gap),
                heatmap,
                adoption,
                noShowRate,
                demand,
                workOrderCounts,
                maintenance,
                dueSoon,
                overdue,
                calibration,
                approvals,
                Collections.emptyList()
        );
    }

    private List<Resource> fetchResources(
            Long institutionId, Long departmentId) {

        String jpql = """
                SELECT r FROM Resource r
                WHERE r.institution.id = :institutionId
                  AND r.isActive = true
                """;

        if (departmentId != null) {
            jpql += " AND r.department.id = :departmentId";
        }

        TypedQuery<Resource> query =
                entityManager.createQuery(jpql, Resource.class);

        query.setParameter("institutionId", institutionId);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        return query.getResultList();
    }

    private double calculateUsage(
            Long institutionId,
            Long departmentId,
            LocalDateTime start,
            LocalDateTime end,
            Map<Long, Double> usageByResource,
            Map<String, Double> heatmapValues
    ) {
        if (!end.isAfter(start)) {
            return 0;
        }

        String jpql = """
                SELECT u FROM UsageLog u
                JOIN FETCH u.resource r
                WHERE r.institution.id = :institutionId
                  AND u.actualStartTime < :end
                  AND (u.actualEndTime IS NULL OR u.actualEndTime > :start)
                """;

        if (departmentId != null) {
            jpql += " AND r.department.id = :departmentId";
        }

        TypedQuery<UsageLog> query =
                entityManager.createQuery(jpql, UsageLog.class);

        query.setParameter("institutionId", institutionId);
        query.setParameter("start", start);
        query.setParameter("end", end);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        double total = 0;

        for (UsageLog log : query.getResultList()) {
            LocalDateTime usageStart = log.getActualStartTime().isBefore(start)
                    ? start : log.getActualStartTime();

            LocalDateTime actualEnd = log.getActualEndTime() == null
                    ? end : log.getActualEndTime();

            LocalDateTime usageEnd = actualEnd.isAfter(end)
                    ? end : actualEnd;

            if (!usageEnd.isAfter(usageStart)) {
                continue;
            }

            double hours = Duration.between(
                    usageStart, usageEnd).toSeconds() / 3600.0;

            Long resourceId = log.getResource().getId();
            usageByResource.merge(resourceId, hours, Double::sum);
            total += hours;

            LocalDateTime cursor = usageStart;

            while (cursor.isBefore(usageEnd)) {
                LocalDateTime nextHour = cursor
                        .withMinute(0).withSecond(0).withNano(0)
                        .plusHours(1);

                LocalDateTime segmentEnd =
                        nextHour.isBefore(usageEnd)
                                ? nextHour : usageEnd;

                double segmentHours = Duration.between(
                        cursor, segmentEnd).toSeconds() / 3600.0;

                String key = cursor.toLocalDate() + "_" + cursor.getHour();
                heatmapValues.merge(key, segmentHours, Double::sum);
                cursor = segmentEnd;
            }
        }

        return total;
    }

    private List<ManagerDashboardResponse.HeatmapCell> buildHeatmap(
            Map<String, Double> values) {

        List<ManagerDashboardResponse.HeatmapCell> result =
                new ArrayList<>();

        values.forEach((key, hours) -> {
            String[] parts = key.split("_");
            result.add(new ManagerDashboardResponse.HeatmapCell(
                    LocalDate.parse(parts[0]),
                    Integer.parseInt(parts[1]),
                    BigDecimal.valueOf(hours).setScale(
                            2, RoundingMode.HALF_UP)
            ));
        });

        result.sort(Comparator
                .comparing(ManagerDashboardResponse.HeatmapCell::date)
                .thenComparingInt(
                        ManagerDashboardResponse.HeatmapCell::hour));

        return result;
    }

    private long countBookings(
            Long institutionId,
            Long departmentId,
            LocalDateTime start,
            LocalDateTime end,
            String status
    ) {
        String jpql = """
                SELECT COUNT(b) FROM Booking b
                WHERE b.resource.institution.id = :institutionId
                  AND b.createdAt >= :start
                  AND b.createdAt < :end
                """;

        if (departmentId != null) {
            jpql += " AND b.resource.department.id = :departmentId";
        }
        if (status != null) {
            jpql += " AND b.status = :status";
        }

        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("institutionId", institutionId)
                .setParameter("start", start)
                .setParameter("end", end);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }
        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getSingleResult();
    }

    private long countAdoptedResources(
            Long institutionId,
            Long departmentId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        String jpql = """
                SELECT COUNT(DISTINCT b.resource.id)
                FROM Booking b
                WHERE b.resource.institution.id = :institutionId
                  AND b.createdAt >= :start
                  AND b.createdAt < :end
                """;

        if (departmentId != null) {
            jpql += " AND b.resource.department.id = :departmentId";
        }

        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("institutionId", institutionId)
                .setParameter("start", start)
                .setParameter("end", end);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        return query.getSingleResult();
    }

    private List<ManagerDashboardResponse.EquipmentDemand> buildHighDemand(
            List<Resource> resources,
            Long institutionId,
            Long departmentId,
            LocalDateTime start,
            LocalDateTime end,
            Map<Long, Double> usageByResource
    ) {
        List<ManagerDashboardResponse.EquipmentDemand> result =
                new ArrayList<>();

        for (Resource resource : resources) {
            Long resourceId = resource.getId();

            Long bookingCount = entityManager.createQuery("""
                    SELECT COUNT(b) FROM Booking b
                    WHERE b.resource.id = :resourceId
                      AND b.createdAt >= :start
                      AND b.createdAt < :end
                    """, Long.class)
                    .setParameter("resourceId", resourceId)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();

            Long waitlistCount = entityManager.createQuery("""
                    SELECT COUNT(w) FROM BookingWaitlist w
                    WHERE w.resource.id = :resourceId
                      AND w.createdAt >= :start
                      AND w.status IN ('WAITING', 'OFFERED')
                    """, Long.class)
                    .setParameter("resourceId", resourceId)
                    .setParameter("start", start)
                    .getSingleResult();

            if (bookingCount == 0 && waitlistCount == 0) {
                continue;
            }

            int quantity = resource.getQuantity() == null
                    ? 1 : Math.max(1, resource.getQuantity());

            double capacity = Duration.between(start, end).toHours()
                    * quantity;

            BigDecimal utilization = percentage(
                    usageByResource.getOrDefault(resourceId, 0.0),
                    capacity);

            result.add(new ManagerDashboardResponse.EquipmentDemand(
                    resourceId,
                    resource.getName(),
                    resource.getCategory(),
                    bookingCount,
                    waitlistCount,
                    utilization
            ));
        }

        result.sort(Comparator
                .comparingLong(ManagerDashboardResponse.EquipmentDemand::waitlistCount)
                .reversed()
                .thenComparing(
                        ManagerDashboardResponse.EquipmentDemand::bookingCount,
                        Comparator.reverseOrder()));

        return result.stream().limit(10).toList();
    }

    private Map<String, Long> getWorkOrderCounts(
            Long institutionId, Long departmentId) {

        String jpql = """
                SELECT w.status, COUNT(w)
                FROM WorkOrder w
                WHERE w.resource.institution.id = :institutionId
                """;

        if (departmentId != null) {
            jpql += " AND w.resource.department.id = :departmentId";
        }

        jpql += " GROUP BY w.status";

        var query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("institutionId", institutionId);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] row : query.getResultList()) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        return result;
    }

    private List<ManagerDashboardResponse.EquipmentMaintenance>
    getActiveMaintenance(Long institutionId, Long departmentId) {

        String jpql = """
                SELECT w FROM WorkOrder w
                JOIN FETCH w.resource r
                WHERE r.institution.id = :institutionId
                  AND w.status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
                """;

        if (departmentId != null) {
            jpql += " AND r.department.id = :departmentId";
        }

        jpql += " ORDER BY w.createdAt DESC";

        var query = entityManager.createQuery(jpql, WorkOrder.class)
                .setParameter("institutionId", institutionId)
                .setMaxResults(20);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        return query.getResultList().stream()
                .map(w -> new ManagerDashboardResponse.EquipmentMaintenance(
                        w.getResource().getId(),
                        w.getResource().getName(),
                        w.getResource().getStatus(),
                        w.getId(),
                        w.getStatus().name()
                ))
                .toList();
    }

    private List<ManagerDashboardResponse.PreventiveSchedule> getSchedules(
            Long institutionId,
            Long departmentId,
            LocalDate from,
            LocalDate to,
            boolean overdue
    ) {
        String jpql = """
                SELECT s FROM PreventiveMaintenanceSchedule s
                JOIN FETCH s.resource r
                WHERE r.institution.id = :institutionId
                  AND s.active = true
                """;

        if (overdue) {
            jpql += " AND s.nextDueAt < :to";
        } else {
            jpql += " AND s.nextDueAt >= :from AND s.nextDueAt <= :to";
        }

        if (departmentId != null) {
            jpql += " AND r.department.id = :departmentId";
        }

        jpql += " ORDER BY s.nextDueAt ASC";

        var query = entityManager.createQuery(
                        jpql, PreventiveMaintenanceSchedule.class)
                .setParameter("institutionId", institutionId)
                .setParameter("to", to.atTime(23, 59, 59));

        if (!overdue) {
            query.setParameter("from", from.atStartOfDay());
        }
        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        return query.getResultList().stream()
                .map(s -> new ManagerDashboardResponse.PreventiveSchedule(
                        s.getId(),
                        s.getResource().getId(),
                        s.getResource().getName(),
                        s.getNextDueAt().toLocalDate(),
                        overdue ? "OVERDUE" : "DUE_SOON"
                ))
                .toList();
    }

    private ManagerDashboardResponse.CalibrationOverview
    getCalibrationOverview(
            Long institutionId,
            Long departmentId,
            LocalDate today
    ) {
        String jpql = """
                SELECT r FROM Resource r
                WHERE r.institution.id = :institutionId
                  AND r.isActive = true
                """;

        if (departmentId != null) {
            jpql += " AND r.department.id = :departmentId";
        }

        var query = entityManager.createQuery(jpql, Resource.class)
                .setParameter("institutionId", institutionId);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        long valid = 0;
        long dueSoon = 0;
        long expired = 0;
        long none = 0;

        for (Resource resource : query.getResultList()) {
            List<CalibrationRecord> records = entityManager.createQuery("""
                    SELECT c FROM CalibrationRecord c
                    WHERE c.resource.id = :resourceId
                    ORDER BY c.calibrationDate DESC, c.id DESC
                    """, CalibrationRecord.class)
                    .setParameter("resourceId", resource.getId())
                    .setMaxResults(1)
                    .getResultList();

            if (records.isEmpty()) {
                none++;
                continue;
            }

            LocalDate dueDate = records.get(0).getNextDueDate();

            if (dueDate.isBefore(today)) {
                expired++;
            } else if (!dueDate.isAfter(today.plusDays(30))) {
                dueSoon++;
            } else {
                valid++;
            }
        }

        return new ManagerDashboardResponse.CalibrationOverview(
                valid, dueSoon, expired, none);
    }

    private List<ManagerDashboardResponse.BookingApproval>
    getPendingApprovals(Long institutionId, Long departmentId) {

        String jpql = """
                SELECT b FROM Booking b
                JOIN FETCH b.user
                JOIN FETCH b.resource r
                WHERE r.institution.id = :institutionId
                  AND b.status = 'PENDING'
                """;

        if (departmentId != null) {
            jpql += " AND r.department.id = :departmentId";
        }

        jpql += " ORDER BY b.createdAt ASC";

        var query = entityManager.createQuery(jpql, Booking.class)
                .setParameter("institutionId", institutionId)
                .setMaxResults(10);

        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }

        return query.getResultList().stream()
                .map(b -> new ManagerDashboardResponse.BookingApproval(
                        b.getId(),
                        b.getResource().getId(),
                        b.getResource().getName(),
                        b.getUser().getFullName(),
                        b.getStartTime(),
                        b.getEndTime(),
                        b.getStatus()
                ))
                .toList();
    }

    private BigDecimal percentage(double numerator, double denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP);
    }
}