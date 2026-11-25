
package com.labresource.platform.resource.controller;

import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.CalibrationRecordRepository;
import com.labresource.platform.resource.repository.ResourceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/resources/{resourceId}/calibrations")
public class CalibrationController {

    private static final int DUE_SOON_DAYS = 30;

    private final CalibrationRecordRepository calibrationRepository;
    private final ResourceRepository resourceRepository;

    public CalibrationController(
            CalibrationRecordRepository calibrationRepository,
            ResourceRepository resourceRepository) {
        this.calibrationRepository = calibrationRepository;
        this.resourceRepository = resourceRepository;
    }

    public record CalibrationRequest(
            LocalDate calibrationDate,
            LocalDate nextDueDate,
            String certificateNumber,
            String certifiedBy,
            String remarks
    ) {}

    public record CalibrationResponse(
            Long id,
            Long resourceId,
            LocalDate calibrationDate,
            LocalDate nextDueDate,
            Long daysUntilDue,
            String certificateNumber,
            String certifiedBy,
            String status,
            String remarks
    ) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<CalibrationResponse> getCalibrations(
            @PathVariable Long resourceId) {

        getResource(resourceId);

        return calibrationRepository
                .findByResource_IdOrderByCalibrationDateDesc(resourceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    @Transactional
    public CalibrationResponse addCalibration(
            @PathVariable Long resourceId,
            @RequestBody CalibrationRequest request) {

        Resource resource = getResource(resourceId);
        validate(request);

        CalibrationRecord record = new CalibrationRecord();
        record.setResource(resource);
        record.setCalibrationDate(request.calibrationDate());
        record.setNextDueDate(request.nextDueDate());
        record.setCertificateNumber(request.certificateNumber());
        record.setCertifiedBy(request.certifiedBy());
        record.setRemarks(request.remarks());

        return toResponse(calibrationRepository.save(record));
    }

    private Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resource not found"
                ));
    }

    private void validate(CalibrationRequest request) {

        if (request == null
                || request.calibrationDate() == null
                || request.nextDueDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Calibration date and next due date are required"
            );
        }

        if (request.nextDueDate().isBefore(request.calibrationDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Next due date cannot be before calibration date"
            );
        }

        if (request.calibrationDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Calibration date cannot be in the future"
            );
        }
    }

    private CalibrationResponse toResponse(CalibrationRecord record) {

        LocalDate today = LocalDate.now();
        LocalDate dueDate = record.getNextDueDate();

        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

        String status;

        if (dueDate.isBefore(today)) {
            status = "EXPIRED";
        } else if (daysUntilDue <= DUE_SOON_DAYS) {
            status = "DUE_SOON";
        } else {
            status = "VALID";
        }

        return new CalibrationResponse(
                record.getId(),
                record.getResource().getId(),
                record.getCalibrationDate(),
                dueDate,
                daysUntilDue,
                record.getCertificateNumber(),
                record.getCertifiedBy(),
                status,
                record.getRemarks()
        );
    }
}
