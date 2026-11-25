package com.labresource.platform.notification.service;

import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates in-app notification records for work-order lifecycle events.
 * Called from WorkOrderService methods that already run in a transaction,
 * so notification records participate in the same database transaction.
 */
@Service
public class WorkOrderNotificationService {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public WorkOrderNotificationService(
            NotificationService notificationService,
            UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyWorkOrderCreated(WorkOrder workOrder) {
        if (workOrder == null || workOrder.getResource() == null
                || workOrder.getResource().getInstitution() == null) {
            return;
        }

        Long institutionId = workOrder.getResource().getInstitution().getId();
        if (institutionId == null) {
            return;
        }

        String title = workOrder.getType() == null
                ? "New work order created"
                : workOrder.getType().name() + " work order created";
        String message = "Work order " + workOrder.getWorkOrderNumber()
                + " has been created for "
                + safeResourceName(workOrder) + ".";

        for (User technician : userRepository
                .findActiveTechniciansByInstitutionId(institutionId)) {
            create(technician, title, message,
                    WorkOrderNotificationType.CREATED, workOrder);
        }
    }

    @Transactional
    public void notifyTechnicianAssigned(WorkOrder workOrder, User technician) {
        if (technician == null) {
            return;
        }
        create(technician,
                "Work order assigned to you",
                "You have been assigned work order "
                        + safeWorkOrderNumber(workOrder) + " for "
                        + safeResourceName(workOrder) + ".",
                WorkOrderNotificationType.ASSIGNED,
                workOrder);
    }

    @Transactional
    public void notifyWorkOrderStarted(WorkOrder workOrder, User actor) {
        notifyInterestedUsers(workOrder, actor,
                "Work order started",
                "Work order " + safeWorkOrderNumber(workOrder) + " is now in progress.",
                WorkOrderNotificationType.STARTED);
    }

    @Transactional
    public void notifyWorkOrderCompleted(WorkOrder workOrder, User actor) {
        notifyInterestedUsers(workOrder, actor,
                "Work order completed",
                "Work order " + safeWorkOrderNumber(workOrder) + " has been completed.",
                WorkOrderNotificationType.COMPLETED);
    }

    @Transactional
    public void notifyWorkOrderCancelled(WorkOrder workOrder, User actor) {
        notifyInterestedUsers(workOrder, actor,
                "Work order cancelled",
                "Work order " + safeWorkOrderNumber(workOrder) + " has been cancelled.",
                WorkOrderNotificationType.CANCELLED);
    }

    private void notifyInterestedUsers(
            WorkOrder workOrder,
            User actor,
            String title,
            String message,
            String type) {

        if (workOrder == null) {
            return;
        }

        Map<Long, User> recipients = new LinkedHashMap<>();
        User creator = workOrder.getCreatedBy();
        User assigned = workOrder.getAssignedTechnician();

        addRecipient(recipients, creator);
        addRecipient(recipients, assigned);

        // Avoid sending the actor a duplicate notification for their own action.
        if (actor != null) {
            recipients.remove(actor.getId());
        }

        for (User recipient : recipients.values()) {
            create(recipient, title, message, type, workOrder);
        }
    }

    private void addRecipient(Map<Long, User> recipients, User user) {
        if (user != null && user.getId() != null) {
            recipients.put(user.getId(), user);
        }
    }

    private void create(
            User recipient,
            String title,
            String message,
            String type,
            WorkOrder workOrder) {

        if (recipient == null || recipient.getId() == null || workOrder == null
                || workOrder.getId() == null) {
            return;
        }

        notificationService.createNotification(
                recipient.getId(),
                title,
                message,
                type,
                WorkOrderNotificationType.REFERENCE_TYPE,
                workOrder.getId()
        );
    }

    private String safeWorkOrderNumber(WorkOrder workOrder) {
        return workOrder == null || workOrder.getWorkOrderNumber() == null
                ? "(not available)" : workOrder.getWorkOrderNumber();
    }

    private String safeResourceName(WorkOrder workOrder) {
        return workOrder == null || workOrder.getResource() == null
                || workOrder.getResource().getName() == null
                ? "the equipment" : workOrder.getResource().getName();
    }
}
