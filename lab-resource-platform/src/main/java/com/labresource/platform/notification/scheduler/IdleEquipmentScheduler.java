package com.labresource.platform.notification.scheduler;

import com.labresource.platform.notification.service.IdleEquipmentNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdleEquipmentScheduler {

    private final IdleEquipmentNotificationService notificationService;

    public IdleEquipmentScheduler(
            IdleEquipmentNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(
            cron = "0 30 6 * * *",
            zone = "Asia/Kolkata"
    )
    public void checkIdleEquipment() {

        int count = notificationService.sendIdleEquipmentAlerts();

        System.out.println(
                "Idle equipment alerts generated: " + count
        );
    }
}