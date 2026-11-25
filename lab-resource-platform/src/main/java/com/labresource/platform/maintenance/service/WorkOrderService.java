package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.AssignTechnicianRequest;
import com.labresource.platform.maintenance.dto.MaintenanceStartPreviewResponse;
import com.labresource.platform.maintenance.dto.CreateWorkOrderRequest;
import com.labresource.platform.maintenance.dto.UpdateWorkOrderRequest;
import com.labresource.platform.maintenance.dto.WorkOrderResponse;
import com.labresource.platform.maintenance.dto.WorkOrderNoteResponse;
import com.labresource.platform.maintenance.dto.WorkOrderServiceLogResponse;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.entity.WorkOrderNote;
import com.labresource.platform.maintenance.entity.WorkOrderServiceLog;
import com.labresource.platform.maintenance.entity.PreventiveMaintenanceSchedule;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.repository.WorkOrderNoteRepository;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.maintenance.repository.WorkOrderServiceLogRepository;
import com.labresource.platform.maintenance.repository.PreventiveMaintenanceScheduleRepository;
import com.labresource.platform.booking.entity.Booking;
import com.labresource.platform.booking.entity.BookingStatusHistory;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.booking.repository.BookingStatusHistoryRepository;
import com.labresource.platform.resource.service.LiveStatusEventPublisher;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;
import com.labresource.platform.notification.service.WorkOrderNotificationService;

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
public class WorkOrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<WorkOrderStatus> ACTIVE_STATUSES =
            List.of(
                    WorkOrderStatus.OPEN,
                    WorkOrderStatus.ASSIGNED,
                    WorkOrderStatus.IN_PROGRESS
            );

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderServiceLogRepository serviceLogRepository;
    private final WorkOrderNoteRepository noteRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository bookingHistoryRepository;
    private final MaintenanceWaitlistService maintenanceWaitlistService;
    private final LiveStatusEventPublisher liveStatusEventPublisher;
    private final PreventiveMaintenanceScheduleRepository scheduleRepository;
    private final WorkOrderNotificationService workOrderNotificationService;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            WorkOrderServiceLogRepository serviceLogRepository,
            WorkOrderNoteRepository noteRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            BookingRepository bookingRepository,
            BookingStatusHistoryRepository bookingHistoryRepository,
            MaintenanceWaitlistService maintenanceWaitlistService,
            LiveStatusEventPublisher liveStatusEventPublisher,
            PreventiveMaintenanceScheduleRepository scheduleRepository,
            WorkOrderNotificationService workOrderNotificationService) {

        this.workOrderRepository = workOrderRepository;
        this.serviceLogRepository = serviceLogRepository;
        this.noteRepository = noteRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.maintenanceWaitlistService = maintenanceWaitlistService;
        this.liveStatusEventPublisher = liveStatusEventPublisher;
        this.scheduleRepository = scheduleRepository;
        this.workOrderNotificationService = workOrderNotificationService;
    }

    // CREATE WORK ORDER

    @Transactional
    public WorkOrderResponse createWorkOrder(
            CreateWorkOrderRequest request,
            String email) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        if (request == null) {
            throw new IllegalArgumentException("Work order request is required");
        }

        Resource resource = resourceRepository
                .findById(request.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resource not found: " + request.getResourceId()));

        requireSameInstitution(manager, resource);

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot create work order for an inactive resource");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Work order title is required");
        }

        WorkOrderType type = parseType(request.getType());

        validateSchedule(
                request.getScheduledStart(),
                request.getScheduledEnd()
        );

        if (request.getEstimatedCost() != null
                && request.getEstimatedCost().signum() < 0) {
            throw new IllegalArgumentException(
                    "Estimated cost cannot be negative");
        }

        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderNumber(generateWorkOrderNumber());
        workOrder.setResource(resource);
        workOrder.setType(type);
        workOrder.setStatus(WorkOrderStatus.OPEN);
        workOrder.setTitle(request.getTitle().trim());
        workOrder.setDescription(request.getDescription());
        workOrder.setCreatedBy(manager);
        workOrder.setScheduledStart(request.getScheduledStart());
        workOrder.setScheduledEnd(request.getScheduledEnd());
        workOrder.setExpectedEnd(request.getScheduledEnd());
        workOrder.setEstimatedCost(request.getEstimatedCost());

        WorkOrder saved = workOrderRepository.save(workOrder);

        saveStatusLog(
                saved,
                null,
                WorkOrderStatus.OPEN,
                manager,
                "Work order created"
        );

        workOrderNotificationService.notifyWorkOrderCreated(saved);

        return WorkOrderResponse.fromEntity(saved);
    }

    // LIST WORK ORDERS FOR INSTITUTION MANAGER

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> getInstitutionWorkOrders(
            String email,
            WorkOrderStatus status,
            WorkOrderType type,
            Long resourceId,
            Pageable pageable) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        if (manager.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Institution is not assigned to this manager");
        }

        return workOrderRepository.findInstitutionWorkOrders(
                manager.getInstitution().getId(),
                status,
                type,
                resourceId,
                pageable
        ).map(WorkOrderResponse::fromEntity);
    }

    // LIST WORK ORDERS ASSIGNED TO TECHNICIAN

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> getMyAssignedWorkOrders(
            String email,
            Pageable pageable) {

        User technician = getUser(email);
        requireRole(technician, "LAB_TECHNICIAN");

        return workOrderRepository
                .findByAssignedTechnicianId(technician.getId(), pageable)
                .map(WorkOrderResponse::fromEntity);
    }

    // GET WORK ORDER DETAILS

    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrder(Long workOrderId, String email) {

        User user = getUser(email);
        WorkOrder workOrder = getWorkOrderEntity(workOrderId);

        authorizeWorkOrderAccess(user, workOrder);

        return WorkOrderResponse.fromEntity(workOrder);
    }

    // ASSIGN OR REASSIGN TECHNICIAN

    @Transactional
    public WorkOrderResponse assignTechnician(
            Long workOrderId,
            AssignTechnicianRequest request,
            String email) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        if (request == null || request.getTechnicianId() == null) {
            throw new IllegalArgumentException("Technician ID is required");
        }

        WorkOrder workOrder = getWorkOrderEntity(workOrderId);
        requireSameInstitution(manager, workOrder.getResource());

        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED
                || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Completed or cancelled work orders cannot be assigned");
        }

        User technician = userRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Technician not found: " + request.getTechnicianId()));

        requireRole(technician, "LAB_TECHNICIAN");
        requireSameInstitution(manager, technician);

        WorkOrderStatus oldStatus = workOrder.getStatus();

        workOrder.setAssignedTechnician(technician);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);

        WorkOrder saved = workOrderRepository.save(workOrder);

        saveStatusLog(
                saved,
                oldStatus,
                WorkOrderStatus.ASSIGNED,
                manager,
                "Assigned to technician: " + technician.getFullName()
        );

        workOrderNotificationService.notifyTechnicianAssigned(saved, technician);

        return WorkOrderResponse.fromEntity(saved);
    }

    // PREVIEW BOOKINGS AFFECTED BY MAINTENANCE

    @Transactional
    public MaintenanceStartPreviewResponse previewMaintenanceStart(
            Long workOrderId,
            LocalDateTime requestedExpectedEnd,
            String email) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        WorkOrder workOrder = getWorkOrderEntity(workOrderId);
        requireSameInstitution(manager, workOrder.getResource());

        if (workOrder.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new IllegalArgumentException(
                    "Only assigned work orders can be previewed before starting");
        }

        LocalDateTime maintenanceStart = LocalDateTime.now();

        LocalDateTime maintenanceEnd = requestedExpectedEnd != null
                ? requestedExpectedEnd
                : workOrder.getExpectedEnd() != null
                  ? workOrder.getExpectedEnd()
                  : workOrder.getScheduledEnd();

        if (maintenanceEnd == null
                || !maintenanceEnd.isAfter(maintenanceStart)) {
            throw new IllegalArgumentException(
                    "A future expected maintenance end time is required");
        }

        List<String> cancellableStatuses = List.of(
                "PENDING",
                "PENDING_APPROVAL",
                "APPROVED",
                "CONFIRMED"
        );

        List<Booking> affectedBookings =
                bookingRepository.findOverlappingBookingsForMaintenance(
                        workOrder.getResource().getId(),
                        cancellableStatuses,
                        maintenanceStart,
                        maintenanceEnd
                );

        List<MaintenanceStartPreviewResponse.AffectedBooking> bookingDetails =
                affectedBookings.stream()
                        .map(booking ->
                                new MaintenanceStartPreviewResponse.AffectedBooking(
                                        booking.getId(),
                                        booking.getUser() == null
                                                ? null : booking.getUser().getFullName(),
                                        booking.getUser() == null
                                                ? null : booking.getUser().getEmail(),
                                        booking.getStartTime(),
                                        booking.getEndTime(),
                                        booking.getStatus()
                                ))
                        .toList();

        Resource resource = workOrder.getResource();

        return new MaintenanceStartPreviewResponse(
                workOrder.getId(),
                workOrder.getWorkOrderNumber(),
                resource.getId(),
                resource.getName(),
                maintenanceStart,
                maintenanceEnd,
                bookingDetails.size(),
                bookingDetails
        );
    }

    // START WORK ORDER

    @Transactional
    public WorkOrderResponse startWorkOrder(
            Long workOrderId,
            UpdateWorkOrderRequest request,
            String email) {

        User actor = getUser(email);
        WorkOrder workOrder = getWorkOrderEntity(workOrderId);

        authorizeAssignedTechnicianOrManager(actor, workOrder);

        if (workOrder.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new IllegalArgumentException(
                    "Only assigned work orders can be started");
        }

        Resource resource = resourceRepository
                .findByIdForUpdate(workOrder.getResource().getId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime maintenanceEnd = request != null
                && request.getExpectedEnd() != null
                ? request.getExpectedEnd()
                : workOrder.getExpectedEnd() != null
                  ? workOrder.getExpectedEnd()
                  : workOrder.getScheduledEnd();

        if (maintenanceEnd == null) {
            throw new IllegalArgumentException(
                    "Expected maintenance end time is required before starting");
        }

        validateExpectedEnd(now, maintenanceEnd);

        if ("OUT_OF_SERVICE".equalsIgnoreCase(resource.getStatus())) {
            throw new IllegalArgumentException("This equipment is out of service");
        }

        WorkOrderStatus oldStatus = workOrder.getStatus();

        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrder.setActualStart(now);
        workOrder.setExpectedEnd(maintenanceEnd);

        resource.setStatus("UNDER_MAINTENANCE");

        WorkOrder saved = workOrderRepository.save(workOrder);

        saveStatusLog(
                saved,
                oldStatus,
                WorkOrderStatus.IN_PROGRESS,
                actor,
                request == null ? "Work started" : request.getRemarks()
        );

        workOrderNotificationService.notifyWorkOrderStarted(saved, actor);

        cancelOverlappingBookings(
                resource.getId(),
                now,
                maintenanceEnd,
                actor.getEmail(),
                "Equipment under maintenance"
        );

        maintenanceWaitlistService.expireOffersForMaintenance(
                resource.getId(),
                now,
                maintenanceEnd
        );

        publishResourceUpdateAfterCommit(
                resource.getId(),
                "UNDER_MAINTENANCE"
        );

        return WorkOrderResponse.fromEntity(saved);
    }

    // COMPLETE WORK ORDER

    @Transactional
    public WorkOrderResponse completeWorkOrder(
            Long workOrderId,
            UpdateWorkOrderRequest request,
            String email) {

        User actor = getUser(email);
        WorkOrder workOrder = getWorkOrderEntity(workOrderId);

        authorizeAssignedTechnicianOrManager(actor, workOrder);

        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only in-progress work orders can be completed");
        }

        if (request != null
                && request.getActualCost() != null
                && request.getActualCost().signum() < 0) {
            throw new IllegalArgumentException("Actual cost cannot be negative");
        }

        WorkOrderStatus oldStatus = workOrder.getStatus();
        LocalDateTime now = LocalDateTime.now();

        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setActualEnd(now);
        workOrder.setCompletedAt(now);

        if (request != null) {
            workOrder.setActualCost(request.getActualCost());
            workOrder.setResolutionNotes(request.getRemarks());
        }

        WorkOrder saved = workOrderRepository.save(workOrder);

        saveStatusLog(
                saved,
                oldStatus,
                WorkOrderStatus.COMPLETED,
                actor,
                request == null ? "Work order completed" : request.getRemarks()
        );

        updatePreventiveScheduleAfterCompletion(saved, now);

        workOrderNotificationService.notifyWorkOrderCompleted(saved, actor);

        restoreResourceIfNoActiveWorkOrders(saved.getResource().getId());

        return WorkOrderResponse.fromEntity(saved);
    }

    // UPDATE PREVENTIVE SCHEDULE AFTER COMPLETION

    private void updatePreventiveScheduleAfterCompletion(
            WorkOrder workOrder,
            LocalDateTime completedAt) {

        if (workOrder.getType() != WorkOrderType.PREVENTIVE) {
            return;
        }

        PreventiveMaintenanceSchedule schedule =
                workOrder.getPreventiveSchedule();

        if (schedule == null) {
            return;
        }

        if (schedule.getRepeatEveryDays() == null
                || schedule.getRepeatEveryDays() <= 0) {
            throw new IllegalStateException(
                    "Preventive schedule repeat interval must be greater than zero");
        }

        schedule.setLastCompletedAt(completedAt);

        schedule.setNextDueAt(
                completedAt.plusDays(schedule.getRepeatEveryDays())
        );

        scheduleRepository.save(schedule);
    }

    // CANCEL WORK ORDER

    @Transactional
    public WorkOrderResponse cancelWorkOrder(
            Long workOrderId,
            UpdateWorkOrderRequest request,
            String email) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        WorkOrder workOrder = getWorkOrderEntity(workOrderId);
        requireSameInstitution(manager, workOrder.getResource());

        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED
                || workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "This work order can no longer be cancelled");
        }

        WorkOrderStatus oldStatus = workOrder.getStatus();

        workOrder.setStatus(WorkOrderStatus.CANCELLED);
        workOrder.setActualEnd(LocalDateTime.now());

        if (request != null && request.getRemarks() != null) {
            workOrder.setResolutionNotes(request.getRemarks());
        }

        WorkOrder saved = workOrderRepository.save(workOrder);

        saveStatusLog(
                saved,
                oldStatus,
                WorkOrderStatus.CANCELLED,
                manager,
                request == null ? "Work order cancelled" : request.getRemarks()
        );

        updatePreventiveScheduleAfterCancellation(saved);

        workOrderNotificationService.notifyWorkOrderCancelled(saved, manager);

        restoreResourceIfNoActiveWorkOrders(saved.getResource().getId());

        return WorkOrderResponse.fromEntity(saved);
    }

    // RESCHEDULE PREVENTIVE MAINTENANCE AFTER CANCELLATION

    private void updatePreventiveScheduleAfterCancellation(WorkOrder workOrder) {

        if (workOrder.getType() != WorkOrderType.PREVENTIVE) {
            return;
        }

        PreventiveMaintenanceSchedule schedule =
                workOrder.getPreventiveSchedule();

        if (schedule == null || !schedule.getActive()) {
            return;
        }

        schedule.setNextDueAt(LocalDateTime.now().plusDays(1));

        scheduleRepository.save(schedule);
    }

    // EXTEND EXPECTED END TIME

    @Transactional
    public WorkOrderResponse extendWorkOrder(
            Long workOrderId,
            UpdateWorkOrderRequest request,
            String email) {

        User manager = getUser(email);
        requireRole(manager, "LAB_MANAGER");

        WorkOrder workOrder = getWorkOrderEntity(workOrderId);
        requireSameInstitution(manager, workOrder.getResource());

        if (request == null || request.getExpectedEnd() == null) {
            throw new IllegalArgumentException("New expected end time is required");
        }

        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only in-progress work orders can be extended");
        }

        validateExpectedEnd(workOrder.getActualStart(), request.getExpectedEnd());

        if (workOrder.getExpectedEnd() != null
                && !request.getExpectedEnd().isAfter(workOrder.getExpectedEnd())) {
            throw new IllegalArgumentException(
                    "New expected end must be later than current expected end");
        }

        LocalDateTime previousExpectedEnd = workOrder.getExpectedEnd();

        workOrder.setExpectedEnd(request.getExpectedEnd());

        WorkOrder saved = workOrderRepository.save(workOrder);

        saveStatusLog(
                saved,
                saved.getStatus(),
                saved.getStatus(),
                manager,
                "Expected end extended to "
                        + request.getExpectedEnd()
                        + ". "
                        + safeRemarks(request.getRemarks())
        );

        if (previousExpectedEnd != null) {
            Resource resource = resourceRepository
                    .findByIdForUpdate(workOrder.getResource().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

            cancelOverlappingBookings(
                    resource.getId(),
                    previousExpectedEnd,
                    request.getExpectedEnd(),
                    manager.getEmail(),
                    "Equipment under maintenance"
            );

            maintenanceWaitlistService.expireOffersForMaintenance(
                    resource.getId(),
                    previousExpectedEnd,
                    request.getExpectedEnd()
            );

            publishResourceUpdateAfterCommit(
                    resource.getId(),
                    "MAINTENANCE_EXTENDED"
            );
        }

        return WorkOrderResponse.fromEntity(saved);
    }

    // ADD NOTE

    @Transactional
    public WorkOrderNoteResponse addNote(
            Long workOrderId,
            String content,
            String email) {

        User actor = getUser(email);
        WorkOrder workOrder = getWorkOrderEntity(workOrderId);

        authorizeWorkOrderAccess(actor, workOrder);

        if (!isRole(actor, "LAB_MANAGER")
                && !isRole(actor, "LAB_TECHNICIAN")) {
            throw new IllegalArgumentException(
                    "You are not allowed to add work order notes");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Note content is required");
        }

        if (content.length() > 2000) {
            throw new IllegalArgumentException("Note cannot exceed 2000 characters");
        }

        WorkOrderNote note = new WorkOrderNote();
        note.setWorkOrder(workOrder);
        note.setAuthor(actor);
        note.setContent(content.trim());

        WorkOrderNote saved = noteRepository.save(note);

        return WorkOrderNoteResponse.fromEntity(saved);
    }

    // GET SERVICE LOGS

    @Transactional(readOnly = true)
    public List<WorkOrderServiceLogResponse> getServiceLogs(
            Long workOrderId,
            String email) {

        User user = getUser(email);
        WorkOrder workOrder = getWorkOrderEntity(workOrderId);

        authorizeWorkOrderAccess(user, workOrder);

        return serviceLogRepository
                .findByWorkOrderIdOrderByChangedAtDesc(workOrderId)
                .stream()
                .map(WorkOrderServiceLogResponse::fromEntity)
                .toList();
    }

    // GET NOTES

    @Transactional(readOnly = true)
    public List<WorkOrderNoteResponse> getNotes(
            Long workOrderId,
            String email) {

        User user = getUser(email);
        WorkOrder workOrder = getWorkOrderEntity(workOrderId);

        authorizeWorkOrderAccess(user, workOrder);

        return noteRepository
                .findByWorkOrderIdOrderByCreatedAtDesc(workOrderId)
                .stream()
                .map(WorkOrderNoteResponse::fromEntity)
                .toList();
    }

    // INTERNAL HELPERS

    private User getUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + email));
    }

    private WorkOrder getWorkOrderEntity(Long id) {
        return workOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Work order not found: " + id));
    }

    private void requireRole(User user, String expectedRole) {
        if (!isRole(user, expectedRole)) {
            throw new IllegalArgumentException(
                    "You are not authorized to perform this action");
        }
    }

    private boolean isRole(User user, String expectedRole) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }

        String actualRole = user.getRole().getName()
                .replace("ROLE_", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        return expectedRole.equals(actualRole);
    }

    private void requireSameInstitution(User first, User second) {
        if (first.getInstitution() == null
                || second.getInstitution() == null
                || !first.getInstitution().getId()
                .equals(second.getInstitution().getId())) {
            throw new IllegalArgumentException(
                    "You can only manage work orders in your institution");
        }
    }

    private void requireSameInstitution(User manager, Resource resource) {
        if (resource.getInstitution() == null
                || manager.getInstitution() == null
                || !manager.getInstitution().getId()
                .equals(resource.getInstitution().getId())) {
            throw new IllegalArgumentException(
                    "You cannot manage resources outside your institution");
        }
    }

    private void authorizeWorkOrderAccess(User user, WorkOrder workOrder) {

        if (isRole(user, "LAB_MANAGER")) {
            requireSameInstitution(user, workOrder.getResource());
            return;
        }

        if (isRole(user, "LAB_TECHNICIAN")) {
            if (workOrder.getAssignedTechnician() == null
                    || !workOrder.getAssignedTechnician().getId()
                    .equals(user.getId())) {
                throw new IllegalArgumentException(
                        "You can only access work orders assigned to you");
            }

            requireSameInstitution(user, workOrder.getResource());
            return;
        }

        throw new IllegalArgumentException(
                "You are not authorized to access this work order");
    }

    private void authorizeAssignedTechnicianOrManager(
            User actor,
            WorkOrder workOrder) {

        if (isRole(actor, "LAB_MANAGER")) {
            requireSameInstitution(actor, workOrder.getResource());
            return;
        }

        if (isRole(actor, "LAB_TECHNICIAN")) {
            if (workOrder.getAssignedTechnician() == null
                    || !workOrder.getAssignedTechnician().getId()
                    .equals(actor.getId())) {
                throw new IllegalArgumentException(
                        "Only the assigned technician can update this work order");
            }

            requireSameInstitution(actor, workOrder.getResource());
            return;
        }

        throw new IllegalArgumentException(
                "You are not authorized to update this work order");
    }

    private void validateSchedule(
            LocalDateTime start,
            LocalDateTime end) {

        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Scheduled end must be after scheduled start");
        }
    }

    private void validateExpectedEnd(
            LocalDateTime start,
            LocalDateTime expectedEnd) {

        if (start != null && !expectedEnd.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Expected end must be after the start time");
        }
    }

    private WorkOrderType parseType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Work order type is required");
        }

        try {
            return WorkOrderType.valueOf(
                    value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid work order type. Allowed: PREVENTIVE, CORRECTIVE");
        }
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
                "Could not generate a unique work order number. Please retry.");
    }

    private void saveStatusLog(
            WorkOrder workOrder,
            WorkOrderStatus oldStatus,
            WorkOrderStatus newStatus,
            User actor,
            String remarks) {

        WorkOrderServiceLog log = new WorkOrderServiceLog();
        log.setWorkOrder(workOrder);
        log.setOldStatus(oldStatus == null ? null : oldStatus.name());
        log.setNewStatus(newStatus.name());
        log.setChangedBy(actor);
        log.setRemarks(remarks);

        serviceLogRepository.save(log);
    }

    // CANCEL OVERLAPPING BOOKINGS AND SAVE HISTORY

    private int cancelOverlappingBookings(
            Long resourceId,
            LocalDateTime maintenanceStart,
            LocalDateTime maintenanceEnd,
            String changedBy,
            String reason) {

        List<String> cancellableStatuses = List.of(
                "PENDING",
                "PENDING_APPROVAL",
                "APPROVED",
                "CONFIRMED"
        );

        List<Booking> affectedBookings =
                bookingRepository.findOverlappingBookingsForMaintenance(
                        resourceId,
                        cancellableStatuses,
                        maintenanceStart,
                        maintenanceEnd
                );

        for (Booking booking : affectedBookings) {
            String oldStatus = booking.getStatus();

            booking.setStatus("CANCELLED");
            booking.setStatusRemark(reason);

            bookingRepository.save(booking);

            BookingStatusHistory history = new BookingStatusHistory();
            history.setBooking(booking);
            history.setOldStatus(oldStatus);
            history.setNewStatus("CANCELLED");
            history.setChangedBy(changedBy);
            history.setRemark(reason);

            bookingHistoryRepository.save(history);
        }

        return affectedBookings.size();
    }

    // RESTORE RESOURCE ONLY WHEN NO ACTIVE WORK ORDERS REMAIN

    private void restoreResourceIfNoActiveWorkOrders(Long resourceId) {

        Resource resource = resourceRepository
                .findByIdForUpdate(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

        if (!"UNDER_MAINTENANCE".equalsIgnoreCase(resource.getStatus())) {
            return;
        }

        List<WorkOrder> activeWorkOrders =
                workOrderRepository.findByResourceIdAndStatusIn(
                        resourceId,
                        ACTIVE_STATUSES
                );

        if (activeWorkOrders.isEmpty()) {
            resource.setStatus("AVAILABLE");

            publishResourceUpdateAfterCommit(
                    resourceId,
                    "MAINTENANCE_COMPLETED"
            );
        }
    }

    // PUBLISH LIVE STATUS AFTER TRANSACTION COMMIT

    private void publishResourceUpdateAfterCommit(
            Long resourceId,
            String eventType) {

        Runnable publish = () ->
                liveStatusEventPublisher.publishResourceUpdate(
                        resourceId,
                        eventType
                );

        if (org.springframework.transaction.support
                .TransactionSynchronizationManager.isSynchronizationActive()) {

            org.springframework.transaction.support
                    .TransactionSynchronizationManager.registerSynchronization(
                            new org.springframework.transaction.support
                                    .TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    publish.run();
                                }
                            }
                    );
        } else {
            publish.run();
        }
    }

    private String safeRemarks(String remarks) {
        return remarks == null ? "" : remarks.trim();
    }
}