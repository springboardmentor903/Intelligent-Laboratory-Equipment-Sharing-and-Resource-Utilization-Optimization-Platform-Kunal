
package com.labresource.platform.resource.repository;

import com.labresource.platform.resource.entity.CalibrationRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalibrationRecordRepository
        extends JpaRepository<CalibrationRecord, Long> {

    List<CalibrationRecord> findByResource_IdOrderByCalibrationDateDesc(
            Long resourceId
    );

    @Query(
            value = """
            SELECT
                r.id,
                r.name,
                d.id,
                d.name,
                c.calibrationDate,
                c.nextDueDate,
                CASE
                    WHEN c.id IS NULL THEN 'NONE'
                    WHEN c.nextDueDate < :today THEN 'EXPIRED'
                    WHEN c.nextDueDate <= :dueSoonLimit THEN 'DUE_SOON'
                    ELSE 'VALID'
                END
            FROM Resource r
            LEFT JOIN r.department d
            LEFT JOIN CalibrationRecord c
                ON c.resource = r
                AND c.id = (
                    SELECT MAX(c2.id)
                    FROM CalibrationRecord c2
                    WHERE c2.resource = r
                    AND c2.calibrationDate = (
                        SELECT MAX(c3.calibrationDate)
                        FROM CalibrationRecord c3
                        WHERE c3.resource = r
                    )
                )
            WHERE r.institution.id = :institutionId
              AND r.isActive = true
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (
                    CAST(:keyword AS string) IS NULL
                    OR LOWER(r.name) LIKE CONCAT(
                        '%', LOWER(CAST(:keyword AS string)), '%'
                    )
                    OR LOWER(d.name) LIKE CONCAT(
                        '%', LOWER(CAST(:keyword AS string)), '%'
                    )
              )
              AND (
                    :state IS NULL
                    OR (:state = 'NONE' AND c.id IS NULL)
                    OR (:state = 'EXPIRED' AND c.nextDueDate < :today)
                    OR (
                        :state = 'DUE_SOON'
                        AND c.nextDueDate >= :today
                        AND c.nextDueDate <= :dueSoonLimit
                    )
                    OR (
                        :state = 'VALID'
                        AND c.nextDueDate > :dueSoonLimit
                    )
              )
            """,
            countQuery = """
            SELECT COUNT(r)
            FROM Resource r
            LEFT JOIN r.department d
            WHERE r.institution.id = :institutionId
              AND r.isActive = true
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (
                    CAST(:keyword AS string) IS NULL
                    OR LOWER(r.name) LIKE CONCAT(
                        '%', LOWER(CAST(:keyword AS string)), '%'
                    )
                    OR LOWER(d.name) LIKE CONCAT(
                        '%', LOWER(CAST(:keyword AS string)), '%'
                    )
              )
              AND (
                    :state IS NULL
                    OR (
                        :state = 'NONE'
                        AND NOT EXISTS (
                            SELECT c FROM CalibrationRecord c
                            WHERE c.resource = r
                        )
                    )
                    OR (
                        :state = 'EXPIRED'
                        AND EXISTS (
                            SELECT c FROM CalibrationRecord c
                            WHERE c.resource = r
                              AND c.id = (
                                  SELECT MAX(c2.id)
                                  FROM CalibrationRecord c2
                                  WHERE c2.resource = r
                                    AND c2.calibrationDate = (
                                        SELECT MAX(c3.calibrationDate)
                                        FROM CalibrationRecord c3
                                        WHERE c3.resource = r
                                    )
                              )
                              AND c.nextDueDate < :today
                        )
                    )
                    OR (
                        :state = 'DUE_SOON'
                        AND EXISTS (
                            SELECT c FROM CalibrationRecord c
                            WHERE c.resource = r
                              AND c.id = (
                                  SELECT MAX(c2.id)
                                  FROM CalibrationRecord c2
                                  WHERE c2.resource = r
                                    AND c2.calibrationDate = (
                                        SELECT MAX(c3.calibrationDate)
                                        FROM CalibrationRecord c3
                                        WHERE c3.resource = r
                                    )
                              )
                              AND c.nextDueDate >= :today
                              AND c.nextDueDate <= :dueSoonLimit
                        )
                    )
                    OR (
                        :state = 'VALID'
                        AND EXISTS (
                            SELECT c FROM CalibrationRecord c
                            WHERE c.resource = r
                              AND c.id = (
                                  SELECT MAX(c2.id)
                                  FROM CalibrationRecord c2
                                  WHERE c2.resource = r
                                    AND c2.calibrationDate = (
                                        SELECT MAX(c3.calibrationDate)
                                        FROM CalibrationRecord c3
                                        WHERE c3.resource = r
                                    )
                              )
                              AND c.nextDueDate > :dueSoonLimit
                        )
                    )
              )
            """
    )
    Page<Object[]> findCalibrationOverview(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            @Param("state") String state,
            @Param("today") LocalDate today,
            @Param("dueSoonLimit") LocalDate dueSoonLimit,
            Pageable pageable
    );
    @Query("""
        SELECT
            CASE
                WHEN c.id IS NULL THEN 'NONE'
                WHEN c.nextDueDate < :today THEN 'EXPIRED'
                WHEN c.nextDueDate <= :dueSoonLimit THEN 'DUE_SOON'
                ELSE 'VALID'
            END,
            COUNT(r)
        FROM Resource r
        LEFT JOIN CalibrationRecord c
          ON c.resource = r
         AND c.id = (
             SELECT MAX(c2.id)
             FROM CalibrationRecord c2
             WHERE c2.resource = r
               AND c2.calibrationDate = (
                   SELECT MAX(c3.calibrationDate)
                   FROM CalibrationRecord c3
                   WHERE c3.resource = r
               )
         )
        WHERE r.institution.id = :institutionId
          AND r.isActive = true
          AND (:departmentId IS NULL OR r.department.id = :departmentId)
        GROUP BY
            CASE
                WHEN c.id IS NULL THEN 'NONE'
                WHEN c.nextDueDate < :today THEN 'EXPIRED'
                WHEN c.nextDueDate <= :dueSoonLimit THEN 'DUE_SOON'
                ELSE 'VALID'
            END
    """)
    List<Object[]> countDashboardCalibrationStates(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("today") LocalDate today,
            @Param("dueSoonLimit") LocalDate dueSoonLimit
    );
}
