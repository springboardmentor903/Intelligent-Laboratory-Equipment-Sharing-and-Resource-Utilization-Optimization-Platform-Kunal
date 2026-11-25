package com.labresource.platform.notification.service;

import com.labresource.platform.booking.entity.UsageLog;
import com.labresource.platform.booking.repository.UsageLogRepository;
import com.labresource.platform.booking.repository.BookingRepository;
import com.labresource.platform.notification.entity.EquipmentAlertLog;
import com.labresource.platform.notification.repository.EquipmentAlertLogRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
public class IdleEquipmentNotificationService {

    private final ResourceRepository resourceRepository;
    private final UsageLogRepository usageLogRepository;
    private final BookingRepository bookingRepository;
    private final EquipmentAlertLogRepository alertRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public IdleEquipmentNotificationService(
            ResourceRepository resourceRepository,
            UsageLogRepository usageLogRepository,
            BookingRepository bookingRepository,
            EquipmentAlertLogRepository alertRepository,
            NotificationService notificationService,
            UserRepository userRepository) {

        this.resourceRepository = resourceRepository;
        this.usageLogRepository = usageLogRepository;
        this.bookingRepository = bookingRepository;
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public int sendIdleEquipmentAlerts() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime idleLimit = now.minusDays(30);

        WeekFields weekFields = WeekFields.of(Locale.ROOT);

        String weekKey = now.getYear() + "-W"
                + String.format(
                "%02d",
                now.get(weekFields.weekOfWeekBasedYear())
        );

        int count = 0;

        List<Resource> resources = resourceRepository.findAll();

        for (Resource resource : resources) {

            if (!Boolean.TRUE.equals(resource.getIsActive())) {
                continue;
            }

            List<UsageLog> logs =
                    usageLogRepository.findByResourceIdOrderByActualStartTimeDesc(
                            resource.getId()
                    );

            LocalDateTime lastUsedAt = logs.isEmpty()
                    ? resource.getCreatedAt()
                    : logs.get(0).getActualStartTime();

            if (lastUsedAt == null || lastUsedAt.isAfter(idleLimit)) {
                continue;
            }

            if (bookingRepository
                    .findFirstByResourceIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                            resource.getId(),
                            "APPROVED",
                            now
                    ).isPresent()) {
                continue;
            }

            if (alertRepository
                    .existsByAlertTypeAndReferenceIdAndPeriodKey(
                            "IDLE_EQUIPMENT",
                            resource.getId(),
                            weekKey)) {
                continue;
            }

            Long institutionId = resource.getInstitution().getId();

            User manager = userRepository
                    .findFirstByInstitution_IdAndRole_NameAndIsActiveTrueOrderByIdAsc(
                            institutionId,
                            "LAB_MANAGER"
                    )
                    .orElse(null);

            if (manager == null) {
                continue;
            }

            notificationService.createNotification(
                    manager.getId(),
                    "Idle Equipment Alert",
                    resource.getName()
                            + " has not been used for more than 30 days. "
                            + "Last usage: " + lastUsedAt,
                    "IDLE_EQUIPMENT"
            );

            EquipmentAlertLog log = new EquipmentAlertLog();
            log.setAlertType("IDLE_EQUIPMENT");
            log.setReferenceId(resource.getId());
            log.setPeriodKey(weekKey);

            alertRepository.save(log);
            count++;
        }

        return count;
    }
}