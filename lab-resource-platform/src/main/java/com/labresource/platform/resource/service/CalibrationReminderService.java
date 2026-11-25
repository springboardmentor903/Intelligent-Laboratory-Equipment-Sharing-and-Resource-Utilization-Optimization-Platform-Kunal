
package com.labresource.platform.resource.service;

import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.entity.WorkOrderServiceLog;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.maintenance.repository.WorkOrderServiceLogRepository;
import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.resource.repository.CalibrationReminderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class CalibrationReminderService {

    private static final int DUE_SOON_DAYS = 30;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<WorkOrderStatus> ACTIVE_STATUSES =
            List.of(
                    WorkOrderStatus.OPEN,
                    WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS
            );

    private final CalibrationReminderRepository calibrationRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderServiceLogRepository serviceLogRepository;

    public CalibrationReminderService(
            CalibrationReminderRepository calibrationRepository,
            WorkOrderRepository workOrderRepository,
            WorkOrderServiceLogRepository serviceLogRepository) {

        this.calibrationRepository = calibrationRepository;
        this.workOrderRepository = workOrderRepository;
        this.serviceLogRepository = serviceLogRepository;
    }

    @Transactional
    public int createDueCalibrationWorkOrders() {

        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(DUE_SOON_DAYS);

        List<CalibrationRecord> dueRecords =
                calibrationRepository.findLatestDueCalibrations(dueDate);

        int createdCount = 0;

        for (CalibrationRecord record : dueRecords) {

            Long resourceId = record.getResource().getId();

            boolean alreadyExists =
                    workOrderRepository.existsByResourceIdAndTypeAndStatusIn(
                            resourceId,
                            WorkOrderType.CALIBRATION,
                            ACTIVE_STATUSES
                    );

            if (alreadyExists) {
                continue;
            }

            WorkOrder workOrder = new WorkOrder();

            workOrder.setWorkOrderNumber(generateWorkOrderNumber());
            workOrder.setResource(record.getResource());
            workOrder.setType(WorkOrderType.CALIBRATION);
            workOrder.setStatus(WorkOrderStatus.OPEN);
            workOrder.setTitle("Calibration due - " + record.getResource().getName());
            workOrder.setDescription(
                    "Calibration is due on " + record.getNextDueDate()
                            + ". Latest calibration date: "
                            + record.getCalibrationDate()
            );

            WorkOrder savedWorkOrder =
                    workOrderRepository.save(workOrder);

            WorkOrderServiceLog log = new WorkOrderServiceLog();
            log.setWorkOrder(savedWorkOrder);
            log.setOldStatus(null);
            log.setNewStatus(WorkOrderStatus.OPEN.name());
            log.setChangedBy(null);
            log.setRemarks("Automatically generated calibration work order");

            serviceLogRepository.save(log);

            createdCount++;
        }

        return createdCount;
    }

    private String generateWorkOrderNumber() {

        int year = Year.now().getValue();

        for (int attempt = 0; attempt < 20; attempt++) {

            int number = RANDOM.nextInt(900000) + 100000;

            String candidate = String.format(
                    Locale.ROOT,
                    "WO-%d-%06d",
                    year,
                    number
            );

            if (!workOrderRepository.existsByWorkOrderNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique work order number"
        );
    }
}
