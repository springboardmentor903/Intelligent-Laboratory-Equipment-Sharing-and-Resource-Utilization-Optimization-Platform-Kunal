
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.entity.PreventiveMaintenanceSchedule;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.entity.WorkOrderServiceLog;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.repository.PreventiveMaintenanceScheduleRepository;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.maintenance.repository.WorkOrderServiceLogRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Component
public class PreventiveMaintenanceScheduler {

    private final PreventiveMaintenanceScheduleRepository scheduleRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderServiceLogRepository serviceLogRepository;

    private final SecureRandom random = new SecureRandom();

    public PreventiveMaintenanceScheduler(
            PreventiveMaintenanceScheduleRepository scheduleRepository,
            WorkOrderRepository workOrderRepository,
            WorkOrderServiceLogRepository serviceLogRepository) {

        this.scheduleRepository = scheduleRepository;
        this.workOrderRepository = workOrderRepository;
        this.serviceLogRepository = serviceLogRepository;
    }

    // Runs every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void generatePreventiveWorkOrders() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueLimit = now.plusDays(7);

        List<PreventiveMaintenanceSchedule> dueSchedules =
                scheduleRepository.findActiveSchedulesDueBefore(dueLimit);

        for (PreventiveMaintenanceSchedule schedule : dueSchedules) {

            if (schedule.getNextDueAt() == null) {
                continue;
            }

            Long resourceId = schedule.getResource().getId();

            boolean activeWorkOrderExists =
                    workOrderRepository.existsByResourceIdAndStatusIn(
                            resourceId,
                            List.of(
                                    WorkOrderStatus.OPEN,
                                    WorkOrderStatus.ASSIGNED,
                                    WorkOrderStatus.IN_PROGRESS
                            )
                    );

            if (activeWorkOrderExists) {
                continue;
            }

            createPreventiveWorkOrder(schedule, now);
        }
    }

    private void createPreventiveWorkOrder(
            PreventiveMaintenanceSchedule schedule,
            LocalDateTime now) {

        LocalDateTime dueDate = schedule.getNextDueAt();

        // For overdue schedules, use the current time.
        LocalDateTime scheduledStart =
                dueDate.isBefore(now) ? now : dueDate;

        LocalDateTime expectedEnd = scheduledStart.plusHours(2);

        WorkOrder workOrder = new WorkOrder();

        workOrder.setWorkOrderNumber(generateWorkOrderNumber());
        workOrder.setResource(schedule.getResource());
        workOrder.setType(WorkOrderType.PREVENTIVE);
        workOrder.setStatus(WorkOrderStatus.OPEN);

        workOrder.setTitle(
                "Preventive Maintenance - " + schedule.getTitle()
        );

        workOrder.setDescription(schedule.getDescription());
        workOrder.setCreatedBy(schedule.getCreatedBy());

        workOrder.setPreventiveSchedule(schedule);

        workOrder.setScheduledStart(scheduledStart);
        workOrder.setScheduledEnd(expectedEnd);
        workOrder.setExpectedEnd(expectedEnd);

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);

        WorkOrderServiceLog log = new WorkOrderServiceLog();

        log.setWorkOrder(savedWorkOrder);
        log.setOldStatus(null);
        log.setNewStatus(WorkOrderStatus.OPEN.name());
        log.setChangedBy(schedule.getCreatedBy());
        log.setRemarks(
                "Preventive work order automatically generated from schedule."
        );

        serviceLogRepository.save(log);
    }

    private String generateWorkOrderNumber() {

        String workOrderNumber;

        do {
            int number = 100000 + random.nextInt(900000);

            workOrderNumber = "WO-"
                    + Year.now().getValue()
                    + "-"
                    + number;

        } while (workOrderRepository.existsByWorkOrderNumber(workOrderNumber));

        return workOrderNumber;
    }
}
