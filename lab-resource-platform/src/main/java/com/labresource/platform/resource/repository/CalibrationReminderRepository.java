package com.labresource.platform.resource.repository;

import com.labresource.platform.resource.entity.CalibrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalibrationReminderRepository
        extends JpaRepository<CalibrationRecord, Long> {

    // Latest calibration record for every resource
    @Query("""
        SELECT c
        FROM CalibrationRecord c
        JOIN FETCH c.resource r
        WHERE r.isActive = true
          AND c.calibrationDate = (
              SELECT MAX(c2.calibrationDate)
              FROM CalibrationRecord c2
              WHERE c2.resource = c.resource
          )
          AND c.id = (
              SELECT MAX(c3.id)
              FROM CalibrationRecord c3
              WHERE c3.resource = c.resource
                AND c3.calibrationDate = c.calibrationDate
          )
        ORDER BY c.nextDueDate ASC
        """)
    List<CalibrationRecord> findLatestCalibrations();

    // Keep this method for automatic scheduler
    @Query("""
        SELECT c
        FROM CalibrationRecord c
        JOIN FETCH c.resource r
        WHERE r.isActive = true
          AND c.nextDueDate <= :dueDate
          AND c.calibrationDate = (
              SELECT MAX(c2.calibrationDate)
              FROM CalibrationRecord c2
              WHERE c2.resource = c.resource
          )
          AND c.id = (
              SELECT MAX(c3.id)
              FROM CalibrationRecord c3
              WHERE c3.resource = c.resource
                AND c3.calibrationDate = c.calibrationDate
          )
        ORDER BY c.nextDueDate ASC
        """)
    List<CalibrationRecord> findLatestDueCalibrations(
            @Param("dueDate") LocalDate dueDate
    );
}