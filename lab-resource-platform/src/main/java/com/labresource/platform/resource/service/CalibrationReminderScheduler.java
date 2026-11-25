
package com.labresource.platform.resource.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CalibrationReminderScheduler {

    private final CalibrationReminderService reminderService;

    public CalibrationReminderScheduler(
            CalibrationReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(
            cron = "0 15 1 * * *",
            zone = "Asia/Kolkata"
    )
    public void generateCalibrationReminders() {

        int createdCount =
                reminderService.createDueCalibrationWorkOrders();

        System.out.println(
                "Calibration reminder job completed. Work orders created: "
                        + createdCount
        );
    }
}
