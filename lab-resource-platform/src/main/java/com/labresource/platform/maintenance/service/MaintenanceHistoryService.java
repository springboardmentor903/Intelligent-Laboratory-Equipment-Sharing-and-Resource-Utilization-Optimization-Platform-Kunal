
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.MaintenanceHistoryResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class MaintenanceHistoryService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public MaintenanceHistoryService(
            WorkOrderRepository workOrderRepository,
            UserRepository userRepository) {
        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
    }

    public List<MaintenanceHistoryResponse> getMaintenanceHistory(
            String email,
            Long resourceId,
            LocalDateTime from,
            LocalDateTime to) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String role = user.getRole().getName()
                .toUpperCase(Locale.ROOT)
                .replace("ROLE_", "");

        List<WorkOrder> workOrders;

        if ("LAB_MANAGER".equals(role)
                || "INSTITUTION_ADMIN".equals(role)) {

            if (user.getInstitution() == null) {
                throw new AccessDeniedException(
                        "Your account is not linked to an institution");
            }

            Page<WorkOrder> page =
                    workOrderRepository.findInstitutionWorkOrders(
                            user.getInstitution().getId(),
                            WorkOrderStatus.COMPLETED,
                            null,
                            resourceId,
                            Pageable.unpaged()
                    );

            workOrders = page.getContent();

        } else if ("LAB_TECHNICIAN".equals(role)) {

            Page<WorkOrder> page =
                    workOrderRepository.findByAssignedTechnicianId(
                            user.getId(),
                            Pageable.unpaged()
                    );

            workOrders = page.getContent().stream()
                    .filter(workOrder ->
                            workOrder.getStatus() == WorkOrderStatus.COMPLETED)
                    .filter(workOrder ->
                            resourceId == null
                                    || (workOrder.getResource() != null
                                    && resourceId.equals(
                                    workOrder.getResource().getId())))
                    .toList();

        } else {
            throw new AccessDeniedException(
                    "You are not allowed to view maintenance history");
        }

        return workOrders.stream()
                .filter(workOrder -> matchesDateRange(workOrder, from, to))
                .sorted(Comparator.comparing(
                        WorkOrder::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(MaintenanceHistoryResponse::fromEntity)
                .toList();
    }

    private boolean matchesDateRange(
            WorkOrder workOrder,
            LocalDateTime from,
            LocalDateTime to) {

        LocalDateTime completedAt = workOrder.getCompletedAt();

        if (completedAt == null) {
            return false;
        }

        if (from != null && completedAt.isBefore(from)) {
            return false;
        }

        if (to != null && completedAt.isAfter(to)) {
            return false;
        }

        return true;
    }
}
