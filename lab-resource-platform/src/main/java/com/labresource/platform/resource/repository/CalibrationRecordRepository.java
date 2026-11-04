
package com.labresource.platform.resource.repository;

import com.labresource.platform.resource.entity.CalibrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalibrationRecordRepository
        extends JpaRepository<CalibrationRecord, Long> {

    List<CalibrationRecord> findByResource_IdOrderByCalibrationDateDesc(
            Long resourceId
    );
}
