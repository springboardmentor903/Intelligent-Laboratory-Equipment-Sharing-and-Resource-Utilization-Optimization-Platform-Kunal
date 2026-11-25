
package com.labresource.platform.dashboard.service;

import com.labresource.platform.dashboard.dto.InstitutionSystemDashboardResponse;
import com.labresource.platform.department.entity.Department;
import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.notification.entity.NotificationDeliveryStatus;
import com.labresource.platform.notification.repository.NotificationDeliveryRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class InstitutionSystemDashboardService {

    private final EntityManager em;
    private final UserRepository userRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    public InstitutionSystemDashboardService(
            EntityManager em,
            UserRepository userRepository,
            NotificationDeliveryRepository deliveryRepository
    ) {
        this.em = em;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public InstitutionSystemDashboardResponse getDashboard(
            String email,
            Long institutionId
    ) {
        User currentUser = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String role = currentUser.getRole().getName()
                .replace("ROLE_", "");

        if (!"SYSTEM_ADMIN".equals(role)
                && !"INSTITUTION_ADMIN".equals(role)) {
            throw new SecurityException("Access denied");
        }

        if ("INSTITUTION_ADMIN".equals(role)) {
            if (currentUser.getInstitution() == null) {
                throw new SecurityException("Institution not assigned");
            }
            institutionId = currentUser.getInstitution().getId();
        }

        if (institutionId == null) {
            throw new IllegalArgumentException("institutionId is required");
        }

        final Long scopedInstitutionId = institutionId;

        Long totalUsers = em.createQuery("""
                SELECT COUNT(u)
                FROM User u
                WHERE u.institution.id = :institutionId
                """, Long.class)
                .setParameter("institutionId", scopedInstitutionId)
                .getSingleResult();

        Long activeUsers = em.createQuery("""
                SELECT COUNT(u)
                FROM User u
                WHERE u.institution.id = :institutionId
                  AND u.isActive = true
                """, Long.class)
                .setParameter("institutionId", scopedInstitutionId)
                .getSingleResult();

        Long totalEquipment = em.createQuery("""
                SELECT COUNT(r)
                FROM Resource r
                WHERE r.institution.id = :institutionId
                """, Long.class)
                .setParameter("institutionId", scopedInstitutionId)
                .getSingleResult();

        Long activeEquipment = em.createQuery("""
                SELECT COUNT(r)
                FROM Resource r
                WHERE r.institution.id = :institutionId
                  AND r.isActive = true
                """, Long.class)
                .setParameter("institutionId", scopedInstitutionId)
                .getSingleResult();

        Long totalDepartments = em.createQuery("""
                SELECT COUNT(d)
                FROM Department d
                WHERE d.institution.id = :institutionId
                  AND d.isActive = true
                """, Long.class)
                .setParameter("institutionId", scopedInstitutionId)
                .getSingleResult();

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long failedDeliveries = deliveryRepository
                .findByStatusAndNextRetryAtLessThanEqual(
                        NotificationDeliveryStatus.FAILED,
                        LocalDateTime.now()
                )
                .stream()
                .filter(d -> d.getCreatedAt() != null
                        && !d.getCreatedAt().isBefore(sevenDaysAgo))
                .filter(d -> d.getNotification() != null
                        && d.getNotification().getUserId() != null)
                .filter(d -> {
                    User recipient = em.find(
                            User.class,
                            d.getNotification().getUserId()
                    );

                    return recipient != null
                            && recipient.getInstitution() != null
                            && scopedInstitutionId.equals(
                            recipient.getInstitution().getId()
                    );
                })
                .count();

        Long maintenanceCount = em.createQuery("""
                SELECT COUNT(w)
                FROM WorkOrder w
                WHERE w.resource.institution.id = :institutionId
                  AND w.status IN (
                      com.labresource.platform.maintenance.enums.WorkOrderStatus.OPEN,
                      com.labresource.platform.maintenance.enums.WorkOrderStatus.ASSIGNED,
                      com.labresource.platform.maintenance.enums.WorkOrderStatus.IN_PROGRESS
                  )
                """, Long.class)
                .setParameter("institutionId", scopedInstitutionId)
                .getSingleResult();

        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getInstitution() != null
                        && scopedInstitutionId.equals(
                        u.getInstitution().getId()
                ))
                .toList();

        List<InstitutionSystemDashboardResponse.InstitutionUser> userRows =
                users.stream()
                        .map(u -> new InstitutionSystemDashboardResponse.InstitutionUser(
                                u.getId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getRole() == null
                                        ? null : u.getRole().getName(),
                                Boolean.TRUE.equals(u.getIsActive())
                                        ? "ACTIVE" : "INACTIVE",
                                u.getDepartment() == null
                                        ? null : u.getDepartment().getId(),
                                u.getDepartment() == null
                                        ? null : u.getDepartment().getName(),
                                toLocalDateTime(u.getCreatedAt()),
                                toLocalDateTime(u.getLastLoginAt())
                        ))
                        .toList();

        return new InstitutionSystemDashboardResponse(
                LocalDateTime.now(),
                new InstitutionSystemDashboardResponse.SystemCounts(
                        totalUsers,
                        activeUsers,
                        totalUsers - activeUsers,
                        totalEquipment,
                        activeEquipment,
                        totalDepartments
                ),
                failedDeliveries,
                0,
                maintenanceCount,
                null,
                List.of(),
                userRows
        );
    }

    @Transactional
    public void deactivateUser(
            String adminEmail,
            Long institutionId,
            Long targetUserId
    ) {
        User admin = userRepository.findByEmailWithProfile(adminEmail)
                .orElseThrow(() -> new NoSuchElementException("Admin not found"));

        String adminRole = admin.getRole().getName()
                .replace("ROLE_", "");

        if (!List.of("SYSTEM_ADMIN", "INSTITUTION_ADMIN")
                .contains(adminRole)) {
            throw new SecurityException("Access denied");
        }

        if ("INSTITUTION_ADMIN".equals(adminRole)) {
            if (admin.getInstitution() == null
                    || !institutionId.equals(admin.getInstitution().getId())) {
                throw new SecurityException("Institution scope mismatch");
            }
        }

        User target = em.find(User.class, targetUserId);

        if (target == null) {
            throw new NoSuchElementException("User not found");
        }

        if (target.getInstitution() == null
                || !institutionId.equals(target.getInstitution().getId())) {
            throw new SecurityException("User belongs to another institution");
        }

        String targetRole = target.getRole() == null
                ? "" : target.getRole().getName().replace("ROLE_", "");

        if ("SYSTEM_ADMIN".equals(targetRole)
                || "INSTITUTION_ADMIN".equals(targetRole)) {
            throw new IllegalArgumentException(
                    "Admin accounts cannot be deactivated through this endpoint"
            );
        }

        target.setIsActive(false);
        em.merge(target);
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null
                ? null
                : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }
}
