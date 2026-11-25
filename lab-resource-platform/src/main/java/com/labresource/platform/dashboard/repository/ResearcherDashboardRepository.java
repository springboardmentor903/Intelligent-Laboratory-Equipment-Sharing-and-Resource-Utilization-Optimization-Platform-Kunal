
package com.labresource.platform.dashboard.repository;

import com.labresource.platform.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResearcherDashboardRepository
        extends JpaRepository<Booking, Long> {

    @Query(value = """
        SELECT b.status, COUNT(*)
        FROM bookings b
        WHERE b.user_id = :userId
          AND b.created_at >= :fromDate
          AND b.created_at < :toDate
        GROUP BY b.status
        """, nativeQuery = true)
    List<Object[]> countUserBookingsByStatus(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
        SELECT r.category, COUNT(b.id) AS booking_count
        FROM bookings b
        JOIN resources r ON r.id = b.resource_id
        WHERE b.user_id = :userId
          AND b.created_at >= :fromDate
          AND b.created_at < :toDate
        GROUP BY r.category
        ORDER BY booking_count DESC
        """, nativeQuery = true)
    List<Object[]> findUserTopCategories(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
        SELECT r.id, COUNT(b.id)
        FROM resources r
        LEFT JOIN bookings b
          ON b.resource_id = r.id
         AND b.created_at >= :fromDate
         AND b.created_at < :toDate
        WHERE r.institution_id = :institutionId
          AND r.is_active = true
        GROUP BY r.id
        """, nativeQuery = true)
    List<Object[]> findInstitutionResourceBookingCounts(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
        SELECT
            r.id,
            COALESCE(
                SUM(
                    EXTRACT(EPOCH FROM (
                        LEAST(
                            COALESCE(ul.actual_end_time, :toDate),
                            :toDate
                        ) -
                        GREATEST(ul.actual_start_time, :fromDate)
                    )) / 3600.0
                ), 0
            )
        FROM resources r
        LEFT JOIN usage_logs ul
          ON ul.resource_id = r.id
         AND ul.actual_start_time < :toDate
         AND COALESCE(ul.actual_end_time, :toDate) > :fromDate
        WHERE r.institution_id = :institutionId
          AND r.is_active = true
        GROUP BY r.id
        """, nativeQuery = true)
    List<Object[]> findInstitutionResourceUsageHours(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
