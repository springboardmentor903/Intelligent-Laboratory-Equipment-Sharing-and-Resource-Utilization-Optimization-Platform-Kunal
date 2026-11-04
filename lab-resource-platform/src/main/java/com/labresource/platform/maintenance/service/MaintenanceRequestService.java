
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import com.labresource.platform.maintenance.repository.MaintenanceRequestRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRepository;
    private final ResourceRepository resourceRepository;

    public MaintenanceRequestService(
            MaintenanceRequestRepository maintenanceRepository,
            ResourceRepository resourceRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceRepository.findAllWithResource();
    }

    @Transactional(readOnly = true)
    public MaintenanceRequest getRequestById(Long id) {
        return maintenanceRepository.findByIdWithResource(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Maintenance request not found: " + id));
    }

    @Transactional
    public MaintenanceRequest createRequest(
            Long resourceId,
            String description,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd) {

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }

        if (scheduledStart != null && scheduledEnd != null
                && !scheduledEnd.isAfter(scheduledStart)) {
            throw new IllegalArgumentException(
                    "Scheduled end must be after scheduled start");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Resource not found: " + resourceId));

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot request maintenance for an inactive resource");
        }

        MaintenanceRequest request = new MaintenanceRequest();
        request.setResource(resource);
        request.setDescription(description.trim());
        request.setScheduledStart(scheduledStart);
        request.setScheduledEnd(scheduledEnd);
        request.setStatus("REQUESTED");

        return maintenanceRepository.save(request);
    }

    @Transactional
    public MaintenanceRequest updateStatus(
            Long id, String newStatus, String notes) {

        MaintenanceRequest request = getRequestById(id);

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        String status = newStatus.trim().toUpperCase();

        List<String> allowedStatuses = List.of(
                "REQUESTED",
                "SCHEDULED",
                "IN_PROGRESS",
                "COMPLETED",
                "CANCELLED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid maintenance status: " + status);
        }

        if ("COMPLETED".equals(status)) {
            request.setCompletedAt(LocalDateTime.now());
        }

        request.setStatus(status);
        request.setResolutionNotes(notes);

        return maintenanceRepository.save(request);
    }
}
