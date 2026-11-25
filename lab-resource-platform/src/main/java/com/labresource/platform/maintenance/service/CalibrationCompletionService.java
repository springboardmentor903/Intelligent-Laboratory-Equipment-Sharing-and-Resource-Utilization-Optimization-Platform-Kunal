
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.CalibrationCompletionRequest;
import com.labresource.platform.maintenance.dto.CalibrationCompletionResponse;
import com.labresource.platform.maintenance.dto.UpdateWorkOrderRequest;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.resource.repository.CalibrationRecordRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class CalibrationCompletionService {

    private final WorkOrderService workOrderService;
    private final WorkOrderRepository workOrderRepository;
    private final CalibrationRecordRepository calibrationRepository;

    public CalibrationCompletionService(
            WorkOrderService workOrderService,
            WorkOrderRepository workOrderRepository,
            CalibrationRecordRepository calibrationRepository) {
        this.workOrderService = workOrderService;
        this.workOrderRepository = workOrderRepository;
        this.calibrationRepository = calibrationRepository;
    }

    @Transactional
    public CalibrationCompletionResponse completeCalibration(
            Long workOrderId,
            CalibrationCompletionRequest request,
            String email) {

        if (request == null
                || request.calibrationDate() == null
                || request.nextDueDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Calibration date and next due date are required"
            );
        }

        LocalDate today = LocalDate.now();

        if (request.calibrationDate().isAfter(today)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Calibration date cannot be in the future"
            );
        }

        if (request.nextDueDate().isBefore(request.calibrationDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Next due date cannot be before calibration date"
            );
        }

        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Work order not found"
                ));

        if (workOrder.getType() != WorkOrderType.CALIBRATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This work order is not a calibration work order"
            );
        }

        UpdateWorkOrderRequest completionRequest =
                new UpdateWorkOrderRequest();

        completionRequest.setRemarks(request.remarks());

        // Existing lifecycle handles authorization, status logs
        // and equipment availability restoration.
        workOrderService.completeWorkOrder(
                workOrderId,
                completionRequest,
                email
        );

        CalibrationRecord record = new CalibrationRecord();
        record.setResource(workOrder.getResource());
        record.setCalibrationDate(request.calibrationDate());
        record.setNextDueDate(request.nextDueDate());
        record.setCertificateNumber(request.certificateNumber());
        record.setCertifiedBy(request.certifiedBy());
        record.setRemarks(request.remarks());

        CalibrationRecord saved =
                calibrationRepository.save(record);

        String state;
        long days = ChronoUnit.DAYS.between(today, request.nextDueDate());

        if (days < 0) {
            state = "EXPIRED";
        } else if (days <= 30) {
            state = "DUE_SOON";
        } else {
            state = "VALID";
        }

        WorkOrder completed = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Completed work order not found"
                ));

        return new CalibrationCompletionResponse(
                completed.getId(),
                completed.getWorkOrderNumber(),
                completed.getStatus().name(),
                saved.getId(),
                saved.getCalibrationDate(),
                saved.getNextDueDate(),
                state
        );
    }
}
