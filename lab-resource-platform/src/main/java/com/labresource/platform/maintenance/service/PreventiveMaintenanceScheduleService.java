
package com.labresource.platform.maintenance.service;

import com.labresource.platform.maintenance.dto.CreatePreventiveScheduleRequest;
import com.labresource.platform.maintenance.dto.PreventiveScheduleResponse;
import com.labresource.platform.maintenance.entity.PreventiveMaintenanceSchedule;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;
import com.labresource.platform.maintenance.repository.PreventiveMaintenanceScheduleRepository;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PreventiveMaintenanceScheduleService {

    private final PreventiveMaintenanceScheduleRepository scheduleRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public PreventiveMaintenanceScheduleService(
            PreventiveMaintenanceScheduleRepository scheduleRepository,
            WorkOrderRepository workOrderRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {

        this.scheduleRepository = scheduleRepository;
        this.workOrderRepository = workOrderRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    // CREATE SCHEDULE
    @Transactional
    public PreventiveScheduleResponse createSchedule(
            CreatePreventiveScheduleRequest request,
            String email) {

        User manager = getUser(email);
        requireLabManager(manager);

        if (request == null) {
            throw new IllegalArgumentException("Schedule request is required");
        }

        Resource resource = resourceRepository
                .findById(request.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment not found"));

        requireSameInstitution(manager, resource);

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot create schedule for inactive equipment");
        }

        if (request.getNextDueAt() == null
                || !request.getNextDueAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Next due date must be in the future");
        }

        if (request.getRepeatEveryDays() == null
                || request.getRepeatEveryDays() < 1) {
            throw new IllegalArgumentException(
                    "Repeat interval must be at least one day");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Schedule title is required");
        }

        if (scheduleRepository
                .existsByResourceIdAndTitleIgnoreCaseAndActiveTrue(
                        resource.getId(),
                        request.getTitle().trim())) {
            throw new IllegalArgumentException(
                    "An active schedule with this title already exists");
        }

        PreventiveMaintenanceSchedule schedule =
                new PreventiveMaintenanceSchedule();

        schedule.setResource(resource);
        schedule.setTitle(request.getTitle().trim());
        schedule.setDescription(request.getDescription());
        schedule.setRepeatEveryDays(request.getRepeatEveryDays());
        schedule.setNextDueAt(request.getNextDueAt());
        schedule.setActive(true);
        schedule.setCreatedBy(manager);

        PreventiveMaintenanceSchedule saved =
                scheduleRepository.save(schedule);

        return PreventiveScheduleResponse.fromEntity(saved);
    }

    // GET ALL ACTIVE + INACTIVE SCHEDULES
    @Transactional(readOnly = true)
    public List<PreventiveScheduleResponse> getInstitutionSchedules(
            String email) {

        User manager = getUser(email);
        requireLabManager(manager);

        if (manager.getInstitution() == null) {
            throw new IllegalArgumentException(
                    "Institution is not assigned to this manager");
        }

        return scheduleRepository
                .findAllSchedulesByInstitution(
                        manager.getInstitution().getId())
                .stream()
                .map(PreventiveScheduleResponse::fromEntity)
                .toList();
    }

    // GET EQUIPMENT-WISE SCHEDULES
    @Transactional(readOnly = true)
    public List<PreventiveScheduleResponse> getResourceSchedules(
            Long resourceId,
            String email) {

        User manager = getUser(email);
        requireLabManager(manager);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment not found"));

        requireSameInstitution(manager, resource);

        return scheduleRepository
                .findByResourceIdOrderByNextDueAtAsc(resourceId)
                .stream()
                .map(PreventiveScheduleResponse::fromEntity)
                .toList();
    }

    // GET SINGLE SCHEDULE
    @Transactional(readOnly = true)
    public PreventiveScheduleResponse getSchedule(
            Long scheduleId,
            String email) {

        User manager = getUser(email);
        requireLabManager(manager);

        PreventiveMaintenanceSchedule schedule =
                getScheduleEntity(scheduleId);

        requireSameInstitution(manager, schedule.getResource());

        return PreventiveScheduleResponse.fromEntity(schedule);
    }

    // UPDATE SCHEDULE AND SYNC ITS OPEN PREVENTIVE WORK ORDERS
    @Transactional
    public PreventiveScheduleResponse updateSchedule(
            Long scheduleId,
            CreatePreventiveScheduleRequest request,
            String email) {

        User manager = getUser(email);
        requireLabManager(manager);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Schedule update request is required");
        }

        PreventiveMaintenanceSchedule schedule =
                getScheduleEntity(scheduleId);

        requireSameInstitution(manager, schedule.getResource());

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Schedule title is required");
        }

        if (request.getRepeatEveryDays() == null
                || request.getRepeatEveryDays() < 1) {
            throw new IllegalArgumentException(
                    "Repeat interval must be at least one day");
        }

        if (request.getNextDueAt() == null) {
            throw new IllegalArgumentException(
                    "Next due date is required");
        }

        if (Boolean.TRUE.equals(schedule.getActive())
                && !request.getNextDueAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Next due date must be in the future");
        }

        if (scheduleRepository
                .existsByResourceIdAndTitleIgnoreCaseAndActiveTrue(
                        schedule.getResource().getId(),
                        request.getTitle().trim())
                && !schedule.getTitle().equalsIgnoreCase(
                request.getTitle().trim())) {
            throw new IllegalArgumentException(
                    "An active schedule with this title already exists");
        }

        // Update schedule
        schedule.setTitle(request.getTitle().trim());
        schedule.setDescription(request.getDescription());
        schedule.setRepeatEveryDays(request.getRepeatEveryDays());
        schedule.setNextDueAt(request.getNextDueAt());

        PreventiveMaintenanceSchedule savedSchedule =
                scheduleRepository.save(schedule);

        // Synchronize only OPEN preventive work orders linked to this schedule
        List<WorkOrder> openPreventiveWorkOrders =
                workOrderRepository.findPreventiveWorkOrdersByScheduleAndStatus(
                        scheduleId,
                        WorkOrderType.PREVENTIVE,
                        WorkOrderStatus.OPEN
                );

        LocalDateTime updatedStart = request.getNextDueAt();
        LocalDateTime updatedEnd = updatedStart.plusHours(2);

        for (WorkOrder workOrder : openPreventiveWorkOrders) {
            workOrder.setScheduledStart(updatedStart);
            workOrder.setScheduledEnd(updatedEnd);
            workOrder.setExpectedEnd(updatedEnd);
            workOrder.setTitle(
                    "Preventive Maintenance - " + savedSchedule.getTitle()
            );
            workOrder.setDescription(savedSchedule.getDescription());
        }

        if (!openPreventiveWorkOrders.isEmpty()) {
            workOrderRepository.saveAll(openPreventiveWorkOrders);
        }

        return PreventiveScheduleResponse.fromEntity(savedSchedule);
    }

    // ACTIVATE / DEACTIVATE SCHEDULE
    @Transactional
    public PreventiveScheduleResponse updateScheduleStatus(
            Long scheduleId,
            boolean active,
            String email) {

        User manager = getUser(email);
        requireLabManager(manager);

        PreventiveMaintenanceSchedule schedule =
                getScheduleEntity(scheduleId);

        requireSameInstitution(manager, schedule.getResource());

        schedule.setActive(active);

        return PreventiveScheduleResponse.fromEntity(
                scheduleRepository.save(schedule));
    }

    // INTERNAL HELPERS
    private User getUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"));
    }

    private PreventiveMaintenanceSchedule getScheduleEntity(Long id) {
        return scheduleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Preventive schedule not found"));
    }

    private void requireLabManager(User user) {
        String role = user.getRole() == null
                ? ""
                : user.getRole().getName();

        if (role == null
                || !role.replace("ROLE_", "")
                .trim()
                .equalsIgnoreCase("LAB_MANAGER")) {
            throw new IllegalArgumentException(
                    "Only LAB_MANAGER can manage preventive schedules");
        }
    }

    private void requireSameInstitution(
            User manager,
            Resource resource) {

        if (manager.getInstitution() == null
                || resource.getInstitution() == null
                || !manager.getInstitution().getId()
                .equals(resource.getInstitution().getId())) {
            throw new IllegalArgumentException(
                    "You can manage schedules only for your institution's equipment");
        }
    }
}
