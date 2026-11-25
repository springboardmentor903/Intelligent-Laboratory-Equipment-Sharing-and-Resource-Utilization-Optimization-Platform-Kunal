package com.labresource.platform.notification.scheduler;

import com.labresource.platform.notification.service.CalibrationExpiryNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CalibrationExpiryScheduler {

    private final CalibrationExpiryNotificationService notificationService;

    public CalibrationExpiryScheduler(
            CalibrationExpiryNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(
            cron = "0 0 6 * * *",
            zone = "Asia/Kolkata"
    )
    public void checkExpiredCalibrations() {

        int count = notificationService.sendExpiredCalibrationAlerts();

        System.out.println(
                "Calibration expiry alerts generated: " + count
        );
    }
}