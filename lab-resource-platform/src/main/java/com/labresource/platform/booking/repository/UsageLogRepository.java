package com.labresource.platform.booking.repository;

import com.labresource.platform.booking.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageLogRepository
        extends JpaRepository<UsageLog, Long> {

    Optional<UsageLog> findByBookingIdAndActualEndTimeIsNull(
            Long bookingId
    );

    List<UsageLog> findByUserIdOrderByActualStartTimeDesc(
            Long userId
    );

    List<UsageLog> findByResourceIdOrderByActualStartTimeDesc(
            Long resourceId
    );

    List<UsageLog> findByBookingIdOrderByActualStartTimeDesc(
            Long bookingId
    );
    @Query(value = """
    SELECT
        ul.resource_id,
        COALESCE(
            SUM(
                EXTRACT(
                    EPOCH FROM (
                        LEAST(
                            COALESCE(ul.actual_end_time, :endDateTime),
                            :endDateTime
                        )
                        -
                        GREATEST(
                            ul.actual_start_time,
                            :startDateTime
                        )
                    )
                ) / 3600.0
            ), 0
        )
    FROM usage_logs ul
    JOIN resources r ON r.id = ul.resource_id
    WHERE ul.actual_start_time < :endDateTime
      AND COALESCE(ul.actual_end_time, :endDateTime) > :startDateTime
      AND (
          :scopeType = 'ALL'
          OR (:scopeType = 'INSTITUTION'
              AND r.institution_id = :scopeId)
          OR (:scopeType = 'DEPARTMENT'
              AND r.department_id = :scopeId)
      )
    GROUP BY ul.resource_id
    """, nativeQuery = true)
    List<Object[]> getActualUsageHoursByResource(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId
    );

    @Query(value = """
        SELECT COALESCE(
            SUM(
                EXTRACT(
                    EPOCH FROM (
                        COALESCE(ul.actual_end_time, :now)
                        - ul.actual_start_time
                    )
                )
            ), 0
        )
        FROM usage_logs ul
        WHERE ul.user_id = :userId
          AND ul.actual_start_time >= :fromDate
          AND ul.actual_start_time <= :now
        """, nativeQuery = true)
    Long sumUserActualUsageSeconds(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("now") LocalDateTime now
    );
    @Query(value = """
        SELECT
            ul.resource_id,
            COALESCE(
                SUM(
                    EXTRACT(
                        EPOCH FROM (
                            COALESCE(ul.actual_end_time, :now)
                            - ul.actual_start_time
                        )
                    ) / 3600.0
                ), 0
            )
        FROM usage_logs ul
        JOIN resources r ON r.id = ul.resource_id
        WHERE r.institution_id = :institutionId
          AND ul.actual_start_time >= :fromDate
          AND ul.actual_start_time <= :now
        GROUP BY ul.resource_id
        """, nativeQuery = true)
    List<Object[]> findInstitutionRecentUsageHours(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("now") LocalDateTime now
    );
}