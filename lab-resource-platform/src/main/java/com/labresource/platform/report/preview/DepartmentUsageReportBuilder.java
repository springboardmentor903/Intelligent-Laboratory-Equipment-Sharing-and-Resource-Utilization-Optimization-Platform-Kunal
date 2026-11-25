
package com.labresource.platform.report.preview;

import com.labresource.platform.report.analytics.ResourceAnalyticsResponse;
import com.labresource.platform.report.analytics.ResourceAnalyticsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
public class DepartmentUsageReportBuilder {

    private static final int MAX_ROWS = 10_000;

    private final ResourceAnalyticsService analyticsService;
    private final EntityManager entityManager;

    public DepartmentUsageReportBuilder(
            ResourceAnalyticsService analyticsService,
            EntityManager entityManager
    ) {
        this.analyticsService = analyticsService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public ReportPreviewResponse build(
            LocalDate from,
            LocalDate to,
            String email
    ) {
        ResourceAnalyticsResponse analytics =
                analyticsService.getAnalytics(from, to, email);

        List<ResourceAnalyticsResponse.ResourceIdleMetric> resources =
                analytics.resources();

        Map<Long, DepartmentAggregate> departments =
                new LinkedHashMap<>();

        if (!resources.isEmpty()) {
            List<Long> resourceIds = resources.stream()
                    .map(ResourceAnalyticsResponse.ResourceIdleMetric::resourceId)
                    .filter(Objects::nonNull)
                    .toList();

            if (!resourceIds.isEmpty()) {
                TypedQuery<Object[]> query = entityManager.createQuery(
                        """
                        SELECT r.id, d.id, d.name
                        FROM Resource r
                        LEFT JOIN r.department d
                        WHERE r.id IN :resourceIds
                        """,
                        Object[].class
                );

                query.setParameter("resourceIds", resourceIds);

                Map<Long, DepartmentInfo> departmentByResource =
                        new HashMap<>();

                for (Object[] result : query.getResultList()) {
                    Long resourceId = (Long) result[0];
                    Long departmentId = (Long) result[1];
                    String departmentName = (String) result[2];

                    departmentByResource.put(
                            resourceId,
                            new DepartmentInfo(
                                    departmentId,
                                    departmentName
                            )
                    );
                }

                for (ResourceAnalyticsResponse.ResourceIdleMetric metric :
                        resources) {

                    DepartmentInfo info =
                            departmentByResource.get(metric.resourceId());

                    Long departmentId =
                            info == null ? null : info.id();

                    String departmentName =
                            info == null || info.name() == null
                                    ? "Unassigned"
                                    : info.name();

                    DepartmentAggregate aggregate =
                            departments.computeIfAbsent(
                                    departmentId,
                                    id -> new DepartmentAggregate(
                                            id,
                                            departmentName
                                    )
                            );

                    aggregate.resourceCount++;
                    aggregate.capacityHours += metric.capacityHours();
                    aggregate.actualUsageHours += metric.actualUsageHours();
                    aggregate.idleHours += metric.idleHours();
                }
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> chartCategories = new ArrayList<>();
        List<Double> chartValues = new ArrayList<>();

        double totalCapacity = 0;
        double totalActual = 0;
        double totalIdle = 0;

        for (DepartmentAggregate department : departments.values()) {
            totalCapacity += department.capacityHours;
            totalActual += department.actualUsageHours;
            totalIdle += department.idleHours;

            if (rows.size() < MAX_ROWS) {
                double utilization = department.capacityHours > 0
                        ? department.actualUsageHours
                          / department.capacityHours * 100
                        : 0;

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("departmentId", department.id);
                row.put("departmentName", department.name);
                row.put("resourceCount", department.resourceCount);
                row.put("capacityHours", round(department.capacityHours));
                row.put("actualUsageHours", round(department.actualUsageHours));
                row.put("idleHours", round(department.idleHours));
                row.put("utilizationPercentage", round(utilization));

                rows.add(row);
                chartCategories.add(department.name);
                chartValues.add(round(utilization));
            }
        }

        double overallUtilization = totalCapacity > 0
                ? totalActual / totalCapacity * 100
                : 0;

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("departmentCount", departments.size());
        totals.put("totalResources", resources.size());
        totals.put("totalCapacityHours", round(totalCapacity));
        totals.put("totalActualUsageHours", round(totalActual));
        totals.put("totalIdleHours", round(totalIdle));
        totals.put("utilizationPercentage", round(overallUtilization));

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("name", "Department Utilization");
        chart.put("categories", chartCategories);
        chart.put("values", chartValues);

        return new ReportPreviewResponse(
                "DEPARTMENT_USAGE",
                "Department Usage",
                from,
                to,
                List.of(
                        "departmentId",
                        "departmentName",
                        "resourceCount",
                        "capacityHours",
                        "actualUsageHours",
                        "idleHours",
                        "utilizationPercentage"
                ),
                rows,
                totals,
                departments.size(),
                departments.size() > MAX_ROWS,
                List.of(chart)
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record DepartmentInfo(Long id, String name) {
    }

    private static class DepartmentAggregate {
        private final Long id;
        private final String name;
        private long resourceCount;
        private double capacityHours;
        private double actualUsageHours;
        private double idleHours;

        private DepartmentAggregate(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
