
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.DowntimeRecordResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class DowntimeService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public DowntimeService(
            WorkOrderRepository workOrderRepository,
            UserRepository userRepository) {

        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
    }

    // INSTITUTION DOWNTIME RECORDS - LAB MANAGER

    @Transactional(readOnly = true)
    public List<DowntimeRecordResponse> getInstitutionDowntime(
            String email,
            Long resourceId,
            LocalDateTime from,
            LocalDateTime to) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        if (manager.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Institution is not assigned to this manager");
        }

        validateDateRange(from, to);

        List<WorkOrder> workOrders =
                workOrderRepository.findInstitutionWorkOrders(
                        manager.getInstitution().getId(),
                        WorkOrderStatus.COMPLETED,
                        null,
                        resourceId,
                        Pageable.unpaged()
                ).getContent();

        return workOrders.stream()
                .filter(workOrder -> workOrder.getActualStart() != null)
                .filter(workOrder -> matchesDateRange(workOrder, from, to))
                .map(DowntimeRecordResponse::fromEntity)
                .toList();
    }

    // TECHNICIAN'S OWN DOWNTIME RECORDS

    @Transactional(readOnly = true)
    public List<DowntimeRecordResponse> getMyDowntime(
            String email,
            LocalDateTime from,
            LocalDateTime to) {

        User technician = getUser(email);
        requireRole(technician, "LAB_TECHNICIAN");

        validateDateRange(from, to);

        List<WorkOrder> workOrders =
                workOrderRepository.findByAssignedTechnicianId(
                        technician.getId(),
                        Pageable.unpaged()
                ).getContent();

        return workOrders.stream()
                .filter(workOrder -> workOrder.getActualStart() != null)
                .filter(workOrder -> matchesDateRange(workOrder, from, to))
                .map(DowntimeRecordResponse::fromEntity)
                .toList();
    }

    private boolean matchesDateRange(
            WorkOrder workOrder,
            LocalDateTime from,
            LocalDateTime to) {

        LocalDateTime start = workOrder.getActualStart();

        LocalDateTime end = workOrder.getActualEnd() != null
                ? workOrder.getActualEnd()
                : LocalDateTime.now();

        if (from != null && end.isBefore(from)) {
            return false;
        }

        if (to != null && start.isAfter(to)) {
            return false;
        }

        return true;
    }

    private void validateDateRange(
            LocalDateTime from,
            LocalDateTime to) {

        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException(
                    "End date must be after or equal to start date");
        }
    }

    private User getUser(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + email));
    }

    private void requireRole(User user, String expectedRole) {

        if (user.getRole() == null
                || user.getRole().getName() == null) {
            throw new IllegalArgumentException(
                    "You are not authorized to access downtime records");
        }

        String actualRole = user.getRole().getName()
                .replace("ROLE_", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!expectedRole.equals(actualRole)) {
            throw new IllegalArgumentException(
                    "You are not authorized to access downtime records");
        }
    }
}
