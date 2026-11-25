package com.labresource.platform.dashboard.service;

import com.labresource.platform.dashboard.dto.InstitutionDashboardResponse;
import com.labresource.platform.department.entity.Department;
import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InstitutionDashboardService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final InstitutionDashboardSupplementService supplementService;

    public InstitutionDashboardService(
            EntityManager entityManager,
            UserRepository userRepository,
            InstitutionDashboardSupplementService supplementService
    ) {
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.supplementService = supplementService;
    }

    @Transactional(readOnly = true)
    public InstitutionDashboardResponse getDashboard(
            String email,
            Long requestedInstitutionId
    ) {
        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String role = user.getRole() == null
                ? ""
                : user.getRole().getName().replace("ROLE_", "");

        boolean systemAdmin = "SYSTEM_ADMIN".equals(role);
        Long institutionId;

        if (systemAdmin) {
            if (requestedInstitutionId == null) {
                throw new IllegalArgumentException(
                        "institutionId is required for System Admin");
            }
            institutionId = requestedInstitutionId;
        } else if ("INSTITUTION_ADMIN".equals(role)) {
            if (user.getInstitution() == null) {
                throw new IllegalStateException("Institution not assigned");
            }
            institutionId = user.getInstitution().getId();
        } else {
            throw new SecurityException("Institution dashboard access denied");
        }

        Institution institution = entityManager.find(
                Institution.class, institutionId);

        if (institution == null) {
            throw new NoSuchElementException("Institution not found");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(90);

        List<Department> departments = entityManager.createQuery("""
                SELECT d FROM Department d
                WHERE d.institution.id = :institutionId
                  AND d.isActive = true
                ORDER BY d.name
                """, Department.class)
                .setParameter("institutionId", institutionId)
                .getResultList();

        List<Resource> resources = entityManager.createQuery("""
                SELECT r FROM Resource r
                WHERE r.institution.id = :institutionId
                  AND r.isActive = true
                """, Resource.class)
                .setParameter("institutionId", institutionId)
                .getResultList();

        Map<Long, Double> usageHours = getUsageHours(
                institutionId, from, now);

        List<InstitutionDashboardResponse.DepartmentUtilization>
                departmentRows = new ArrayList<>();

        for (Department department : departments) {
            List<Resource> departmentResources = resources.stream()
                    .filter(r -> r.getDepartment() != null
                            && department.getId().equals(
                            r.getDepartment().getId()))
                    .toList();

            double actualHours = departmentResources.stream()
                    .mapToDouble(r -> usageHours.getOrDefault(r.getId(), 0.0))
                    .sum();

            double capacityHours = departmentResources.stream()
                    .mapToDouble(r -> 90.0 * 24.0
                            * Math.max(1, r.getQuantity() == null
                            ? 1 : r.getQuantity()))
                    .sum();

            BigDecimal utilization = percentage(actualHours, capacityHours);

            BigDecimal target = department.getUtilizationTargetPercent() == null
                    ? institution.getUtilizationTargetPercent()
                    : department.getUtilizationTargetPercent();

            if (target == null) target = BigDecimal.ZERO;

            departmentRows.add(
                    new InstitutionDashboardResponse.DepartmentUtilization(
                            department.getId(),
                            department.getName(),
                            utilization,
                            target,
                            utilization.subtract(target)
                                    .setScale(2, RoundingMode.HALF_UP)
                    )
            );
        }

        List<InstitutionDashboardResponse.MonthlyUtilization>
                monthlyTrends = getMonthlyTrends(
                institutionId, resources, now);

        List<InstitutionDashboardResponse.IdleEquipmentAlert>
                idleAlerts = getIdleAlerts(resources, usageHours, now);

        InstitutionDashboardSupplementService.DashboardSupplement supplement =
                supplementService.build(institutionId);

        return new InstitutionDashboardResponse(
                now,
                institutionId,
                institution.getName(),
                departmentRows,
                monthlyTrends,
                idleAlerts,
                supplement.sharing(),
                supplement.crossDepartment(),
                supplement.costBudget(),
                systemAdmin
        );
    }

    private Map<Long, Double> getUsageHours(
            Long institutionId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    u.resource_id,
                    COALESCE(SUM(
                        EXTRACT(EPOCH FROM (
                            COALESCE(u.actual_end_time, :toDate)
                            - GREATEST(u.actual_start_time, :fromDate)
                        )) / 3600.0
                    ), 0)
                FROM usage_logs u
                JOIN resources r ON r.id = u.resource_id
                WHERE r.institution_id = :institutionId
                  AND u.actual_start_time < :toDate
                  AND COALESCE(u.actual_end_time, :toDate) > :fromDate
                GROUP BY u.resource_id
                """)
                .setParameter("institutionId", institutionId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getResultList();

        Map<Long, Double> result = new HashMap<>();

        for (Object[] row : rows) {
            result.put(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).doubleValue()
            );
        }

        return result;
    }

    private List<InstitutionDashboardResponse.MonthlyUtilization>
    getMonthlyTrends(
            Long institutionId,
            List<Resource> resources,
            LocalDateTime now
    ) {
        List<InstitutionDashboardResponse.MonthlyUtilization> result =
                new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.toLocalDate()
                    .withDayOfMonth(1).minusMonths(i).atStartOfDay();

            LocalDateTime monthEnd = monthStart.plusMonths(1);

            Object value = entityManager.createNativeQuery("""
                    SELECT COALESCE(SUM(
                        EXTRACT(EPOCH FROM (
                            COALESCE(u.actual_end_time, :monthEnd)
                            - u.actual_start_time
                        )) / 3600.0
                    ), 0)
                    FROM usage_logs u
                    JOIN resources r ON r.id = u.resource_id
                    WHERE r.institution_id = :institutionId
                      AND u.actual_start_time >= :monthStart
                      AND u.actual_start_time < :monthEnd
                    """)
                    .setParameter("institutionId", institutionId)
                    .setParameter("monthStart", monthStart)
                    .setParameter("monthEnd", monthEnd)
                    .getSingleResult();

            double hours = ((Number) value).doubleValue();

            double capacity = resources.stream()
                    .mapToDouble(r -> Duration.between(
                            monthStart, monthEnd).toHours()
                            * Math.max(1, r.getQuantity() == null
                            ? 1 : r.getQuantity()))
                    .sum();

            result.add(new InstitutionDashboardResponse.MonthlyUtilization(
                    monthStart.getYear(),
                    monthStart.getMonthValue(),
                    percentage(hours, capacity),
                    decimal(hours),
                    decimal(capacity)
            ));
        }

        return result;
    }

    private List<InstitutionDashboardResponse.IdleEquipmentAlert>
    getIdleAlerts(
            List<Resource> resources,
            Map<Long, Double> usageHours,
            LocalDateTime now
    ) {
        List<InstitutionDashboardResponse.IdleEquipmentAlert> result =
                new ArrayList<>();

        for (Resource resource : resources) {
            LocalDateTime lastUsed = entityManager.createQuery("""
                    SELECT MAX(u.actualStartTime)
                    FROM UsageLog u
                    WHERE u.resource.id = :resourceId
                    """, LocalDateTime.class)
                    .setParameter("resourceId", resource.getId())
                    .getSingleResult();

            long idleDays = lastUsed == null
                    ? 3650
                    : Duration.between(lastUsed, now).toDays();

            if (idleDays < 30) continue;

            int quantity = Math.max(
                    1,
                    resource.getQuantity() == null
                            ? 1 : resource.getQuantity()
            );

            BigDecimal utilization = percentage(
                    usageHours.getOrDefault(resource.getId(), 0.0),
                    90.0 * 24.0 * quantity
            );

            result.add(new InstitutionDashboardResponse.IdleEquipmentAlert(
                    resource.getId(),
                    resource.getName(),
                    resource.getCategory(),
                    lastUsed == null ? null : lastUsed.toLocalDate(),
                    idleDays,
                    utilization
            ));
        }

        result.sort(Comparator.comparingLong(
                InstitutionDashboardResponse.IdleEquipmentAlert::idleDays
        ).reversed());

        return result.stream().limit(20).toList();
    }

    private BigDecimal percentage(double value, double total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(value * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}