
package com.labresource.platform.report.preview;

import com.labresource.platform.report.ResourceUtilizationResponse;
import com.labresource.platform.report.ResourceUtilizationService;
import com.labresource.platform.report.registry.ReportDefinition;
import com.labresource.platform.report.registry.ReportRegistryService;
import com.labresource.platform.resource.entity.Resource;

import jakarta.persistence.EntityManager;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class ReportPreviewService {

    private static final int MAX_PREVIEW_ROWS = 10_000;

    private final ReportRegistryService registryService;
    private final ResourceUtilizationService utilizationService;
    private final DepartmentUsageReportBuilder departmentUsageBuilder;
    private final MaintenanceReportBuilder maintenanceBuilder;
    private final SharingReportBuilder sharingBuilder;
    private final ProcurementCostReportBuilder procurementBuilder;
    private final EntityManager entityManager;

    public ReportPreviewService(
            ReportRegistryService registryService,
            ResourceUtilizationService utilizationService,
            DepartmentUsageReportBuilder departmentUsageBuilder,
            MaintenanceReportBuilder maintenanceBuilder,
            SharingReportBuilder sharingBuilder,
            ProcurementCostReportBuilder procurementBuilder,
            EntityManager entityManager
    ) {
        this.registryService = registryService;
        this.utilizationService = utilizationService;
        this.departmentUsageBuilder = departmentUsageBuilder;
        this.maintenanceBuilder = maintenanceBuilder;
        this.sharingBuilder = sharingBuilder;
        this.procurementBuilder = procurementBuilder;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public ReportPreviewResponse preview(
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            String email
    ) {
        return preview(
                type,
                fromDate,
                toDate,
                email,
                new ReportFilters(null, null, null, null)
        );
    }

    @Transactional(readOnly = true)
    public ReportPreviewResponse preview(
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            String email,
            ReportFilters filters
    ) {
        ReportDefinition definition =
                registryService.getReportDefinition(type, email);

        validateDates(fromDate, toDate);

        ReportPreviewResponse report = switch (definition.type()) {
            case "UTILIZATION" ->
                    buildUtilization(fromDate, toDate, email, definition);

            case "DEPARTMENT_USAGE" ->
                    departmentUsageBuilder.build(fromDate, toDate, email);

            case "MAINTENANCE_DOWNTIME" ->
                    maintenanceBuilder.build(fromDate, toDate, email);

            case "INTER_INSTITUTION_SHARING" ->
                    sharingBuilder.build(fromDate, toDate, email);

            case "PROCUREMENT_COST_ANALYSIS" ->
                    procurementBuilder.build(fromDate, toDate, email);

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported report type"
            );
        };

        return applyFilters(report, filters);
    }

    private ReportPreviewResponse buildUtilization(
            LocalDate from,
            LocalDate to,
            String email,
            ReportDefinition definition
    ) {
        List<ResourceUtilizationResponse> data =
                utilizationService.getResourceUtilization(
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay(),
                        email
                );

        List<Map<String, Object>> rows = new ArrayList<>();

        long totalBookings = 0;
        long completedBookings = 0;
        double bookedHours = 0;
        double actualHours = 0;

        for (ResourceUtilizationResponse item : data) {
            totalBookings += item.getTotalBookings();
            completedBookings += item.getCompletedBookings();
            bookedHours += item.getTotalBookedHours();
            actualHours += item.getActualUsageHours();

            if (rows.size() < MAX_PREVIEW_ROWS) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("resourceId", item.getResourceId());
                row.put("resourceName", item.getResourceName());
                row.put("totalBookings", item.getTotalBookings());
                row.put("completedBookings", item.getCompletedBookings());
                row.put("bookedHours", item.getTotalBookedHours());
                row.put("actualUsageHours", item.getActualUsageHours());
                rows.add(row);
            }
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalBookings", totalBookings);
        totals.put("completedBookings", completedBookings);
        totals.put("bookedHours", bookedHours);
        totals.put("actualUsageHours", actualHours);

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("name", "Resource Actual Usage");
        chart.put(
                "categories",
                rows.stream()
                        .map(row -> String.valueOf(row.get("resourceName")))
                        .toList()
        );
        chart.put(
                "values",
                rows.stream()
                        .map(row -> row.get("actualUsageHours"))
                        .toList()
        );

        return new ReportPreviewResponse(
                definition.type(),
                definition.title(),
                from,
                to,
                List.of(
                        "resourceId",
                        "resourceName",
                        "totalBookings",
                        "completedBookings",
                        "bookedHours",
                        "actualUsageHours"
                ),
                rows,
                totals,
                data.size(),
                data.size() > MAX_PREVIEW_ROWS,
                List.of(chart)
        );
    }

    private ReportPreviewResponse applyFilters(
            ReportPreviewResponse report,
            ReportFilters filters
    ) {
        if (filters == null || filters.isEmpty()) {
            return report;
        }

        Map<Long, Long> departmentByResource = new HashMap<>();
        List<Map<String, Object>> filteredRows = new ArrayList<>();

        for (Map<String, Object> originalRow : report.rows()) {
            Map<String, Object> row = new LinkedHashMap<>(originalRow);

            Long resourceId = asLong(row.get("resourceId"));

            if (resourceId != null && filters.departmentId() != null
                    && row.get("departmentId") == null) {

                Long departmentId = departmentByResource.computeIfAbsent(
                        resourceId,
                        id -> {
                            Resource resource =
                                    entityManager.find(Resource.class, id);

                            if (resource == null
                                    || resource.getDepartment() == null) {
                                return null;
                            }

                            return resource.getDepartment().getId();
                        }
                );

                row.put("departmentId", departmentId);
            }

            if (filters.departmentId() != null) {
                Long rowDepartmentId = asLong(row.get("departmentId"));

                if (!filters.departmentId().equals(rowDepartmentId)) {
                    continue;
                }
            }

            if (filters.resourceId() != null) {
                if (!filters.resourceId().equals(resourceId)) {
                    continue;
                }
            }

            if (filters.status() != null && !filters.status().isBlank()) {
                Object rowStatus = row.get("status");

                if (rowStatus == null
                        || !filters.status().equalsIgnoreCase(
                        rowStatus.toString()
                )) {
                    continue;
                }
            }

            if (filters.keyword() != null
                    && !filters.keyword().isBlank()) {

                String keyword = filters.keyword().trim()
                        .toLowerCase(Locale.ROOT);

                boolean matches = row.values().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(value -> value.toString()
                                .toLowerCase(Locale.ROOT)
                                .contains(keyword));

                if (!matches) {
                    continue;
                }
            }

            filteredRows.add(row);
        }

        if (filteredRows.size() > MAX_PREVIEW_ROWS) {
            filteredRows = filteredRows.subList(0, MAX_PREVIEW_ROWS);
        }

        Map<String, Object> filteredTotals = new LinkedHashMap<>();
        filteredTotals.put("filteredRows", filteredRows.size());

        return new ReportPreviewResponse(
                report.reportType(),
                report.title(),
                report.fromDate(),
                report.toDate(),
                report.columns(),
                filteredRows,
                filteredTotals,
                filteredRows.size(),
                report.truncated(),
                List.of()
        );
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both fromDate and toDate are required"
            );
        }

        if (to.isBefore(from)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "toDate cannot be before fromDate"
            );
        }

        if (from.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate cannot be in the future"
            );
        }

        if (from.plusYears(10).isBefore(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report date range cannot exceed 10 years"
            );
        }
    }
}
