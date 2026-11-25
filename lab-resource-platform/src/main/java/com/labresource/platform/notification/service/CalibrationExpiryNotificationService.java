package com.labresource.platform.notification.service;

import com.labresource.platform.notification.repository.EquipmentAlertLogRepository;
import com.labresource.platform.notification.entity.EquipmentAlertLog;
import com.labresource.platform.resource.entity.CalibrationRecord;
import com.labresource.platform.resource.repository.CalibrationReminderRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CalibrationExpiryNotificationService {

    private final CalibrationReminderRepository calibrationRepository;
    private final EquipmentAlertLogRepository alertRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public CalibrationExpiryNotificationService(
            CalibrationReminderRepository calibrationRepository,
            EquipmentAlertLogRepository alertRepository,
            NotificationService notificationService,
            UserRepository userRepository) {

        this.calibrationRepository = calibrationRepository;
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public int sendExpiredCalibrationAlerts() {

        LocalDate today = LocalDate.now();
        String periodKey = today.toString();
        int sentCount = 0;

        List<CalibrationRecord> records =
                calibrationRepository.findLatestCalibrations();

        for (CalibrationRecord record : records) {

            if (!record.getNextDueDate().isBefore(today)) {
                continue;
            }

            Long recordId = record.getId();

            if (alertRepository
                    .existsByAlertTypeAndReferenceIdAndPeriodKey(
                            "CALIBRATION_EXPIRED",
                            recordId,
                            periodKey)) {
                continue;
            }

            Long institutionId =
                    record.getResource().getInstitution().getId();

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
                    "Calibration Expired",
                    record.getResource().getName()
                            + " calibration expired on "
                            + record.getNextDueDate()
                            + ". Please arrange calibration.",
                    "CALIBRATION_EXPIRED"
            );

            EquipmentAlertLog log = new EquipmentAlertLog();
            log.setAlertType("CALIBRATION_EXPIRED");
            log.setReferenceId(recordId);
            log.setPeriodKey(periodKey);

            alertRepository.save(log);
            sentCount++;
        }

        return sentCount;
    }
}