
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.resource.id = :resourceId
          AND b.status IN :statuses
          AND b.startTime < :maintenanceEnd
          AND b.endTime > :maintenanceStart
        ORDER BY b.startTime ASC
    """)
    List<Booking> findOverlappingBookingsForMaintenance(
            @Param("resourceId") Long resourceId,
            @Param("statuses") List<String> statuses,
            @Param("maintenanceStart") LocalDateTime maintenanceStart,
            @Param("maintenanceEnd") LocalDateTime maintenanceEnd
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
    """)
    List<Booking> findAllWithDetails();

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

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.user.id = :userId
    """)
    List<Booking> findByUserIdWithDetails(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.status = :status
    """)
    List<Booking> findByStatusWithDetails(
            @Param("status") String status
    );

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

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.id = :bookingId
    """)
    Optional<Booking> findByIdWithDetails(
            @Param("bookingId") Long bookingId
    );

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

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.resource
        WHERE b.user.id = :userId
          AND b.startTime >= :now
          AND b.status IN ('PENDING', 'APPROVED', 'IN_USE')
        ORDER BY b.startTime ASC
    """)
    List<Booking> findUpcomingDashboardBookings(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        SELECT b.status, COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
          AND b.createdAt >= :fromDate
        GROUP BY b.status
    """)
    List<Object[]> countUserBookingsByStatusSince(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
          AND b.status = 'NO_SHOW'
          AND b.createdAt >= :fromDate
    """)
    long countUserNoShowsSince(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
        SELECT b.resource.category, COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
          AND b.createdAt >= :fromDate
        GROUP BY b.resource.category
        ORDER BY COUNT(b) DESC
    """)
    List<Object[]> findUserTopBookedCategories(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
        SELECT b.resource.id, COUNT(b)
        FROM Booking b
        WHERE b.resource.institution.id = :institutionId
          AND b.createdAt >= :fromDate
        GROUP BY b.resource.id
        ORDER BY COUNT(b) DESC
    """)
    List<Object[]> findInstitutionPopularResources(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
        SELECT DISTINCT b.resource.id
        FROM Booking b
        WHERE b.user.id = :userId
    """)
    List<Long> findPreviouslyBookedResourceIds(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.resource.institution.id = :institutionId
          AND b.createdAt >= :fromDate
          AND b.createdAt < :toDate
          AND (:departmentId IS NULL OR b.resource.department.id = :departmentId)
    """)
    long countDashboardBookings(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.resource.institution.id = :institutionId
          AND b.status = 'NO_SHOW'
          AND b.createdAt >= :fromDate
          AND b.createdAt < :toDate
          AND (:departmentId IS NULL OR b.resource.department.id = :departmentId)
    """)
    long countDashboardNoShows(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        SELECT b.status, COUNT(b)
        FROM Booking b
        WHERE b.resource.institution.id = :institutionId
          AND b.createdAt >= :fromDate
          AND b.createdAt < :toDate
          AND (:departmentId IS NULL OR b.resource.department.id = :departmentId)
        GROUP BY b.status
    """)
    List<Object[]> countDashboardBookingsByStatus(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        JOIN FETCH b.resource
        WHERE b.resource.institution.id = :institutionId
          AND b.status = 'PENDING'
          AND (:departmentId IS NULL OR b.resource.department.id = :departmentId)
        ORDER BY b.createdAt ASC
    """)
    List<Booking> findDashboardPendingApprovals(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            org.springframework.data.domain.Pageable pageable
    );
}
