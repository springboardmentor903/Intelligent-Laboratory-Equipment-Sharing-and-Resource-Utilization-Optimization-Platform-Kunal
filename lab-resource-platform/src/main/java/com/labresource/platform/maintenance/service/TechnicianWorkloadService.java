
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.TechnicianWorkloadResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TechnicianWorkloadService {

    private static final List<WorkOrderStatus> ACTIVE_STATUSES = List.of(
            WorkOrderStatus.OPEN,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS
    );

    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;

    public TechnicianWorkloadService(
            UserRepository userRepository,
            WorkOrderRepository workOrderRepository) {
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<TechnicianWorkloadResponse> getInstitutionWorkload(
            String email) {

        User manager = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"
                ));

        if (manager.getRole() == null
                || !List.of(
                "LAB_MANAGER",
                "INSTITUTION_ADMIN",
                "SYSTEM_ADMIN"
        ).contains(
                manager.getRole().getName()
                        .replace("ROLE_", "")
                        .toUpperCase()
        )) {
            throw new IllegalArgumentException(
                    "You are not authorized to view technician workload"
            );
        }

        if (manager.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "User is not linked to an institution"
            );
        }

        Long institutionId = manager.getInstitution().getId();
        LocalDateTime now = LocalDateTime.now();

        List<User> technicians =
                userRepository.findActiveTechniciansByInstitution(
                        institutionId
                );

        List<TechnicianWorkloadResponse> result = new ArrayList<>();

        for (User technician : technicians) {

            List<WorkOrder> activeTasks =
                    workOrderRepository.findTechnicianActiveTasks(
                            institutionId,
                            technician.getId(),
                            ACTIVE_STATUSES
                    );

            long inProgress = activeTasks.stream()
                    .filter(task -> task.getStatus()
                            == WorkOrderStatus.IN_PROGRESS)
                    .count();

            long overdue = activeTasks.stream()
                    .filter(task -> {
                        LocalDateTime dueDate = getDueDate(task);
                        return dueDate != null && dueDate.isBefore(now);
                    })
                    .count();

            LocalDateTime nextScheduledDate = activeTasks.stream()
                    .map(this::getNextScheduledDate)
                    .filter(date -> date != null && !date.isBefore(now))
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            result.add(new TechnicianWorkloadResponse(
                    technician.getId(),
                    technician.getFullName(),
                    technician.getEmail(),
                    activeTasks.size(),
                    inProgress,
                    overdue,
                    nextScheduledDate
            ));
        }

        result.sort((first, second) ->
                Long.compare(second.activeTasks(), first.activeTasks()));

        return result;
    }

    private LocalDateTime getDueDate(WorkOrder task) {
        if (task.getScheduledEnd() != null) {
            return task.getScheduledEnd();
        }

        return task.getExpectedEnd();
    }

    private LocalDateTime getNextScheduledDate(WorkOrder task) {
        if (task.getScheduledStart() != null) {
            return task.getScheduledStart();
        }

        return task.getExpectedEnd();
    }
}
