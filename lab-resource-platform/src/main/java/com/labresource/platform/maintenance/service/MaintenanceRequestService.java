
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.entity.WorkOrderServiceLog;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.repository.MaintenanceRequestRepository;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.maintenance.repository.WorkOrderServiceLogRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class MaintenanceRequestService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<String> SUBMITTER_ROLES = List.of(
            "RESEARCHER_STUDENT",
            "DEPARTMENT_HEAD",
            "LAB_TECHNICIAN"
    );

    private final MaintenanceRequestRepository maintenanceRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderServiceLogRepository serviceLogRepository;

    public MaintenanceRequestService(
            MaintenanceRequestRepository maintenanceRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            WorkOrderRepository workOrderRepository,
            WorkOrderServiceLogRepository serviceLogRepository) {

        this.maintenanceRepository = maintenanceRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
        this.serviceLogRepository = serviceLogRepository;
    }

    // ---------------------------------------------------------
    // USER AND ROLE HELPERS
    // ---------------------------------------------------------

    private User getUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user is required");
        }

        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private String getRole(User user) {
        if (user.getRole() == null
                || user.getRole().getName() == null) {
            throw new IllegalArgumentException(
                    "User role is not configured");
        }

        String role = user.getRole().getName()
                .trim()
                .toUpperCase(Locale.ROOT);

        return role.startsWith("ROLE_")
                ? role.substring(5)
                : role;
    }

    private void requireManager(User user, Resource resource) {
        if (!"LAB_MANAGER".equals(getRole(user))) {
            throw new IllegalArgumentException(
                    "Only Lab Managers can review maintenance requests");
        }

        if (user.getInstitution() == null
                || resource.getInstitution() == null
                || !user.getInstitution().getId()
                .equals(resource.getInstitution().getId())) {
            throw new IllegalArgumentException(
                    "You can manage equipment only in your institution");
        }
    }

    // ---------------------------------------------------------
    // CREATE MAINTENANCE REQUEST
    // ---------------------------------------------------------

    @Transactional
    public MaintenanceRequest createRequest(
            String email,
            Long resourceId,
            String title,
            String description,
            String priority) {

        User user = getUser(email);

        if (!SUBMITTER_ROLES.contains(getRole(user))) {
            throw new IllegalArgumentException(
                    "Your role cannot submit maintenance requests");
        }

        if (resourceId == null) {
            throw new IllegalArgumentException("Equipment ID is required");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }

        if (title.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Title cannot exceed 150 characters");
        }

        if (description.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 1000 characters");
        }

        String normalizedPriority = priority == null
                ? "MEDIUM"
                : priority.trim().toUpperCase(Locale.ROOT);

        if (!List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")
                .contains(normalizedPriority)) {
            throw new IllegalArgumentException(
                    "Invalid maintenance priority");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Equipment not found"));

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot request maintenance for inactive equipment");
        }

        MaintenanceRequest request = new MaintenanceRequest();
        request.setResource(resource);
        request.setRequester(user);
        request.setTitle(title.trim());
        request.setDescription(description.trim());
        request.setPriority(normalizedPriority);
        request.setStatus("PENDING");

        return maintenanceRepository.save(request);
    }

    // ---------------------------------------------------------
    // GET MY REQUESTS
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<MaintenanceRequest> getMyRequests(
            String email,
            String status,
            Long resourceId,
            String priority,
            Pageable pageable) {

        User user = getUser(email);

        return maintenanceRepository.findMyRequests(
                user.getId(),
                normalizeFilter(status),
                resourceId,
                normalizeFilter(priority),
                pageable
        );
    }

    // ---------------------------------------------------------
    // GET MANAGER QUEUE
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<MaintenanceRequest> getManagerQueue(
            String email,
            String status,
            Long resourceId,
            String priority,
            Pageable pageable) {

        User manager = getUser(email);

        if (!"LAB_MANAGER".equals(getRole(manager))) {
            throw new IllegalArgumentException(
                    "Only Lab Managers can access the review queue");
        }

        if (manager.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Manager institution is not configured");
        }

        return maintenanceRepository.findInstitutionRequests(
                manager.getInstitution().getId(),
                normalizeFilter(status),
                resourceId,
                normalizeFilter(priority),
                pageable
        );
    }

    // ---------------------------------------------------------
    // GET REQUEST BY ID
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public MaintenanceRequest getRequestById(Long id) {
        return maintenanceRepository.findByIdWithResource(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Maintenance request not found: " + id));
    }

    @Transactional(readOnly = true)
    public MaintenanceRequest getMyRequestById(
            String email,
            Long id) {

        User user = getUser(email);
        MaintenanceRequest request = getRequestById(id);

        if (request.getRequester() == null
                || !request.getRequester().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You cannot access this maintenance request");
        }

        return request;
    }

    // ---------------------------------------------------------
    // REVIEW REQUEST
    // APPROVED REQUEST AUTOMATICALLY CREATES CORRECTIVE WORK ORDER
    // ---------------------------------------------------------

    @Transactional
    public MaintenanceRequest reviewRequest(
            String email,
            Long requestId,
            String decision,
            String remarks) {

        User manager = getUser(email);
        MaintenanceRequest request = getRequestById(requestId);

        requireManager(manager, request.getResource());

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Only pending requests can be reviewed");
        }

        if (decision == null || decision.isBlank()) {
            throw new IllegalArgumentException("Decision is required");
        }

        String normalizedDecision =
                decision.trim().toUpperCase(Locale.ROOT);

        if (!List.of("APPROVED", "REJECTED")
                .contains(normalizedDecision)) {
            throw new IllegalArgumentException(
                    "Decision must be APPROVED or REJECTED");
        }

        if ("REJECTED".equals(normalizedDecision)
                && (remarks == null || remarks.isBlank())) {
            throw new IllegalArgumentException(
                    "Remarks are required when rejecting a request");
        }

        if (remarks != null && remarks.length() > 1000) {
            throw new IllegalArgumentException(
                    "Remarks cannot exceed 1000 characters");
        }

        request.setStatus(normalizedDecision);
        request.setReviewedBy(manager);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewRemarks(
                remarks == null ? null : remarks.trim());

        MaintenanceRequest savedRequest =
                maintenanceRepository.save(request);

        if ("APPROVED".equals(normalizedDecision)) {
            createCorrectiveWorkOrder(savedRequest, manager);
        }

        return savedRequest;
    }

    // ---------------------------------------------------------
    // AUTOMATIC CORRECTIVE WORK ORDER CREATION
    // ---------------------------------------------------------

    private WorkOrder createCorrectiveWorkOrder(
            MaintenanceRequest request,
            User manager) {

        Resource resource = request.getResource();

        WorkOrder workOrder = new WorkOrder();

        workOrder.setWorkOrderNumber(generateWorkOrderNumber());
        workOrder.setResource(resource);
        workOrder.setType(WorkOrderType.CORRECTIVE);
        workOrder.setStatus(WorkOrderStatus.OPEN);

        workOrder.setTitle(
                request.getTitle() == null
                        ? "Corrective maintenance - Resource "
                          + resource.getId()
                        : request.getTitle()
        );

        workOrder.setDescription(request.getDescription());
        workOrder.setCreatedBy(manager);
        workOrder.setMaintenanceRequest(request);

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        saveInitialServiceLog(savedWorkOrder, manager);

        return savedWorkOrder;
    }

    // ---------------------------------------------------------
    // UNIQUE WORK ORDER NUMBER
    // FORMAT: WO-YYYY-000000
    // ---------------------------------------------------------

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
                "Unable to generate a unique work order number");
    }

    // ---------------------------------------------------------
    // INITIAL WORK ORDER SERVICE LOG
    // ---------------------------------------------------------

    private void saveInitialServiceLog(
            WorkOrder workOrder,
            User manager) {

        WorkOrderServiceLog log = new WorkOrderServiceLog();

        log.setWorkOrder(workOrder);
        log.setOldStatus(null);
        log.setNewStatus(WorkOrderStatus.OPEN.name());
        log.setChangedBy(manager);
        log.setRemarks(
                "Corrective work order automatically created "
                        + "after maintenance request approval"
        );

        serviceLogRepository.save(log);
    }

    // ---------------------------------------------------------
    // FILTER NORMALIZATION
    // ---------------------------------------------------------

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()
                || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    // ---------------------------------------------------------
    // LEGACY COMPATIBILITY METHODS
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceRepository.findAllWithResource();
    }

    @Transactional
    public MaintenanceRequest createRequest(
            Long resourceId,
            String description,
            LocalDateTime scheduledStart,
            LocalDateTime scheduledEnd) {

        if (scheduledStart != null && scheduledEnd != null
                && !scheduledEnd.isAfter(scheduledStart)) {
            throw new IllegalArgumentException(
                    "Scheduled end must be after scheduled start");
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found"));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setResource(resource);
        request.setDescription(description);
        request.setTitle("Legacy maintenance request");
        request.setStatus("PENDING");
        request.setScheduledStart(scheduledStart);
        request.setScheduledEnd(scheduledEnd);

        return maintenanceRepository.save(request);
    }

    @Transactional
    public MaintenanceRequest updateStatus(
            Long id,
            String newStatus,
            String notes) {

        MaintenanceRequest request = getRequestById(id);

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        String status = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!"PENDING".equals(status)) {
            throw new IllegalArgumentException(
                    "Approval and rejection must use the authenticated review endpoint");
        }

        request.setStatus(status);
        request.setReviewRemarks(notes);

        return maintenanceRepository.save(request);
    }
}
