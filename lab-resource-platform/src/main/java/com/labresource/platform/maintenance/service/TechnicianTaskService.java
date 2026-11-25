
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.TechnicianTaskResponse;
import com.labresource.platform.maintenance.dto.TechnicianTaskSummaryResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class TechnicianTaskService {

    private static final List<WorkOrderStatus> ACTIVE_STATUSES = List.of(
            WorkOrderStatus.OPEN,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS
    );

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public TechnicianTaskService(
            WorkOrderRepository workOrderRepository,
            UserRepository userRepository) {
        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<TechnicianTaskResponse> getMyTasks(
            String email,
            WorkOrderStatus status,
            Long departmentId,
            String keyword,
            Pageable pageable) {

        User technician = getTechnician(email);

        // Use an empty string instead of NULL so PostgreSQL binds the
        // optional search parameter as VARCHAR (not BYTEA).
        String search = keyword == null ? "" : keyword.trim();

        return workOrderRepository.findTechnicianTasks(
                technician.getInstitution().getId(),
                technician.getId(),
                status,
                departmentId,
                search,
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TechnicianTaskSummaryResponse getMyTaskSummary(String email) {

        User technician = getTechnician(email);

        Long institutionId = technician.getInstitution().getId();
        Long technicianId = technician.getId();

        LocalDateTime now = LocalDateTime.now();

        long open = workOrderRepository.countByAssignedTechnicianIdAndStatusIn(
                technicianId,
                List.of(WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED)
        );

        long inProgress =
                workOrderRepository.countByAssignedTechnicianIdAndStatus(
                        technicianId,
                        WorkOrderStatus.IN_PROGRESS
                );

        long completed =
                workOrderRepository.countByAssignedTechnicianIdAndStatus(
                        technicianId,
                        WorkOrderStatus.COMPLETED
                );

        long overdue = workOrderRepository.countOverdueTechnicianTasks(
                institutionId,
                technicianId,
                ACTIVE_STATUSES,
                now
        );

        List<WorkOrder> activeTasks =
                workOrderRepository.findTechnicianActiveTasks(
                        institutionId,
                        technicianId,
                        ACTIVE_STATUSES
                );

        long overdueGroup = 0;
        long todayGroup = 0;
        long thisWeekGroup = 0;
        long laterGroup = 0;

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        for (WorkOrder task : activeTasks) {

            LocalDateTime dueDate = getDueDate(task);

            if (dueDate == null) {
                laterGroup++;
                continue;
            }

            LocalDate due = dueDate.toLocalDate();

            if (due.isBefore(today)) {
                overdueGroup++;
            } else if (due.isEqual(today)) {
                todayGroup++;
            } else if (!due.isAfter(weekEnd)) {
                thisWeekGroup++;
            } else {
                laterGroup++;
            }
        }

        return new TechnicianTaskSummaryResponse(
                open + inProgress + completed,
                open,
                inProgress,
                overdue,
                completed,
                overdueGroup,
                todayGroup,
                thisWeekGroup,
                laterGroup
        );
    }

    private TechnicianTaskResponse toResponse(WorkOrder workOrder) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = getDueDate(workOrder);

        boolean overdue = dueDate != null
                && dueDate.isBefore(now)
                && ACTIVE_STATUSES.contains(workOrder.getStatus());

        String priority = calculatePriority(dueDate, now, overdue);
        String taskGroup = calculateTaskGroup(dueDate, now, overdue);

        return new TechnicianTaskResponse(
                workOrder.getId(),
                workOrder.getWorkOrderNumber(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getType().name(),
                workOrder.getStatus().name(),
                workOrder.getResource().getName(),
                workOrder.getResource().getId(),
                workOrder.getResource().getDepartment() == null
                        ? null
                        : workOrder.getResource().getDepartment().getName(),
                workOrder.getScheduledStart(),
                workOrder.getScheduledEnd(),
                workOrder.getExpectedEnd(),
                workOrder.getActualStart(),
                priority,
                taskGroup,
                overdue
        );
    }

    private LocalDateTime getDueDate(WorkOrder workOrder) {
        if (workOrder.getScheduledEnd() != null) {
            return workOrder.getScheduledEnd();
        }

        if (workOrder.getExpectedEnd() != null) {
            return workOrder.getExpectedEnd();
        }

        return workOrder.getScheduledStart();
    }

    private String calculatePriority(
            LocalDateTime dueDate,
            LocalDateTime now,
            boolean overdue) {

        if (overdue) {
            return "HIGH";
        }

        if (dueDate == null) {
            return "LOW";
        }

        long hours = ChronoUnit.HOURS.between(now, dueDate);

        if (hours <= 24) {
            return "HIGH";
        }

        if (hours <= 72) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private String calculateTaskGroup(
            LocalDateTime dueDate,
            LocalDateTime now,
            boolean overdue) {

        if (overdue) {
            return "OVERDUE";
        }

        if (dueDate == null) {
            return "LATER";
        }

        LocalDate due = dueDate.toLocalDate();
        LocalDate today = now.toLocalDate();

        if (due.isEqual(today)) {
            return "TODAY";
        }

        if (!due.isBefore(today) && !due.isAfter(today.plusDays(7))) {
            return "THIS_WEEK";
        }

        return "LATER";
    }

    private User getTechnician(String email) {

        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"
                ));

        if (user.getRole() == null
                || !"LAB_TECHNICIAN".equalsIgnoreCase(
                user.getRole().getName().replace("ROLE_", ""))) {
            throw new IllegalArgumentException(
                    "Only technicians can access My Tasks"
            );
        }

        if (user.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Technician is not linked to an institution"
            );
        }

        return user;
    }
}
