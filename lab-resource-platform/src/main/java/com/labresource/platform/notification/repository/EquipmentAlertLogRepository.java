package com.labresource.platform.notification.repository;

import com.labresource.platform.notification.entity.EquipmentAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentAlertLogRepository
        extends JpaRepository<EquipmentAlertLog, Long> {

    boolean existsByAlertTypeAndReferenceIdAndPeriodKey(
            String alertType,
            Long referenceId,
            String periodKey
    );
}