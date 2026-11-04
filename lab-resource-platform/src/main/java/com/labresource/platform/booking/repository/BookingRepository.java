
package com.labresource.platform.booking.repository;

import com.labresource.platform.booking.entity.Booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatus(String status);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByResourceId(Long resourceId);

    // Count overlapping bookings for capacity validation
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.resource.id = :resourceId
        AND b.status IN ('PENDING', 'APPROVED', 'IN_USE')
        AND b.startTime < :endTime
        AND b.endTime > :startTime
    """)
    long countOverlappingBookings(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Fetch all bookings with user and resource details
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
    """)
    List<Booking> findAllWithDetails();

    // Institution Admin: fetch only own institution bookings
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource r
        JOIN r.institution i
        WHERE i.id = :institutionId
    """)
    List<Booking> findByInstitutionIdWithDetails(
            @Param("institutionId") Long institutionId
    );

    // Fetch bookings for a particular user
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.user.id = :userId
    """)
    List<Booking> findByUserIdWithDetails(
            @Param("userId") Long userId
    );

    // Fetch bookings by status
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.status = :status
    """)
    List<Booking> findByStatusWithDetails(
            @Param("status") String status
    );

    // Lock booking while updating its status
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.id = :bookingId
    """)
    Optional<Booking> findByIdWithDetailsForUpdate(
            @Param("bookingId") Long bookingId
    );

    // Fetch booking details
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.id = :bookingId
    """)
    Optional<Booking> findByIdWithDetails(
            @Param("bookingId") Long bookingId
    );

    // Fetch all bookings currently in use
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.resource
        WHERE b.status = 'IN_USE'
    """)
    List<Booking> findAllInUseWithResource();

    Optional<Booking>
    findFirstByResourceIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
            Long resourceId,
            String status,
            LocalDateTime now
    );

    // Resource utilization report
    @Query(value = """
        SELECT
            r.id,
            r.name,
            COUNT(b.id),
            COALESCE(
                SUM(CASE WHEN b.status = 'COMPLETED'
                    THEN 1 ELSE 0 END), 0
            ),
            COALESCE(
                SUM(
                    CASE
                        WHEN b.start_time < :endDateTime
                         AND b.end_time > :startDateTime
                        THEN EXTRACT(
                            EPOCH FROM (
                                LEAST(b.end_time, :endDateTime)
                                -
                                GREATEST(b.start_time, :startDateTime)
                            )
                        ) / 3600.0
                        ELSE 0
                    END
                ), 0
            )
        FROM resources r
        LEFT JOIN bookings b
            ON b.resource_id = r.id
           AND b.start_time < :endDateTime
           AND b.end_time > :startDateTime
        WHERE
            (:scopeType = 'ALL')
            OR (:scopeType = 'INSTITUTION'
                AND r.institution_id = :scopeId)
            OR (:scopeType = 'DEPARTMENT'
                AND r.department_id = :scopeId)
        GROUP BY r.id, r.name
        ORDER BY r.id
        """, nativeQuery = true)
    List<Object[]> getResourceUtilizationReport(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId
    );
}
