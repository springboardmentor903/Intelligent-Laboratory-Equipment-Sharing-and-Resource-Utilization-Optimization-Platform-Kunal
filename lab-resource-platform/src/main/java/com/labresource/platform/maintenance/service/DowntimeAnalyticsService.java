
package com.labresource.platform.maintenance.service;

import com.labresource.platform.institution.entity.Institution;
import com.labresource.platform.maintenance.dto.DowntimeAnalyticsResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DowntimeAnalyticsService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    public DowntimeAnalyticsService(
            WorkOrderRepository workOrderRepository,
            UserRepository userRepository,
            ResourceRepository resourceRepository) {

        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }

    public DowntimeAnalyticsResponse getInstitutionDowntimeAnalytics(
            String email,
            LocalDateTime from,
            LocalDateTime to) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String role = user.getRole().getName()
                .toUpperCase()
                .replace("ROLE_", "");

        if (!"LAB_MANAGER".equals(role)
                && !"INSTITUTION_ADMIN".equals(role)) {
            throw new AccessDeniedException(
                    "You are not allowed to view downtime analytics");
        }

        Institution institution = user.getInstitution();

        if (institution == null) {
            throw new IllegalArgumentException(
                    "User is not linked to an institution");
        }

        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must be after start date");
        }

        List<WorkOrder> workOrders =
                workOrderRepository.findInstitutionWorkOrders(
                        institution.getId(),
                        null,
                        null,
                        null,
                        Pageable.unpaged()
                ).getContent();

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime effectiveFrom = from;

        if (effectiveFrom == null) {
            effectiveFrom = workOrders.stream()
                    .map(WorkOrder::getActualStart)
                    .filter(value -> value != null)
                    .min(LocalDateTime::compareTo)
                    .orElse(now);
        }

        LocalDateTime effectiveTo = to == null ? now : to;

        DowntimeAnalyticsResponse response =
                new DowntimeAnalyticsResponse();

        Map<Long, DowntimeAnalyticsResponse.EquipmentDowntimeSummary>
                equipmentMap = new LinkedHashMap<>();

        long totalDowntime = 0;
        long totalOpenHoursDowntime = 0;
        long totalWorkOrders = 0;

        for (WorkOrder workOrder : workOrders) {

            if (workOrder.getActualStart() == null
                    || workOrder.getResource() == null) {
                continue;
            }

            LocalDateTime actualStart = workOrder.getActualStart();

            LocalDateTime actualEnd =
                    workOrder.getActualEnd() == null
                            ? now
                            : workOrder.getActualEnd();

            if (!actualEnd.isAfter(actualStart)) {
                continue;
            }

            LocalDateTime overlapStart =
                    actualStart.isAfter(effectiveFrom)
                            ? actualStart
                            : effectiveFrom;

            LocalDateTime overlapEnd =
                    actualEnd.isBefore(effectiveTo)
                            ? actualEnd
                            : effectiveTo;

            if (!overlapEnd.isAfter(overlapStart)) {
                continue;
            }

            long downtimeMinutes =
                    java.time.Duration.between(
                            overlapStart,
                            overlapEnd
                    ).toMinutes();

            long openHoursDowntime =
                    WorkingHoursCalculator.calculateOpenMinutes(
                            overlapStart,
                            overlapEnd,
                            institution.getOpeningTime(),
                            institution.getClosingTime(),
                            institution.getWorkingDays()
                    );

            Long resourceId = workOrder.getResource().getId();

            DowntimeAnalyticsResponse.EquipmentDowntimeSummary summary =
                    equipmentMap.computeIfAbsent(resourceId, id -> {
                        DowntimeAnalyticsResponse.EquipmentDowntimeSummary item =
                                new DowntimeAnalyticsResponse.EquipmentDowntimeSummary();

                        item.setResourceId(id);
                        item.setResourceName(
                                workOrder.getResource().getName()
                        );

                        return item;
                    });

            summary.setDowntimeMinutes(
                    summary.getDowntimeMinutes() + downtimeMinutes
            );

            summary.setDowntimeDuringOpenHoursMinutes(
                    summary.getDowntimeDuringOpenHoursMinutes()
                            + openHoursDowntime
            );

            summary.setWorkOrderCount(
                    summary.getWorkOrderCount() + 1
            );

            totalDowntime += downtimeMinutes;
            totalOpenHoursDowntime += openHoursDowntime;
            totalWorkOrders++;
        }

        long activeResourceCount =
                resourceRepository.countActiveResourcesByInstitutionId(
                        institution.getId()
                );

        long openMinutesPerResource =
                WorkingHoursCalculator.calculateOpenMinutes(
                        effectiveFrom,
                        effectiveTo,
                        institution.getOpeningTime(),
                        institution.getClosingTime(),
                        institution.getWorkingDays()
                );

        long totalOpenHoursMinutes =
                openMinutesPerResource * activeResourceCount;

        double averageDowntime = totalWorkOrders == 0
                ? 0.0
                : (double) totalDowntime / totalWorkOrders;

        double openHoursLostPercentage =
                totalOpenHoursMinutes == 0
                        ? 0.0
                        : (double) totalOpenHoursDowntime
                          / totalOpenHoursMinutes * 100.0;

        response.setTotalDowntimeMinutes(totalDowntime);
        response.setDowntimeDuringOpenHoursMinutes(
                totalOpenHoursDowntime
        );
        response.setTotalOpenHoursMinutes(totalOpenHoursMinutes);
        response.setOpenHoursLostPercentage(
                Math.round(openHoursLostPercentage * 100.0) / 100.0
        );
        response.setAverageDowntimeMinutes(
                Math.round(averageDowntime * 100.0) / 100.0
        );
        response.setWorkOrderCount(totalWorkOrders);
        response.setEquipmentDetails(
                List.copyOf(equipmentMap.values())
        );

        return response;
    }
}
