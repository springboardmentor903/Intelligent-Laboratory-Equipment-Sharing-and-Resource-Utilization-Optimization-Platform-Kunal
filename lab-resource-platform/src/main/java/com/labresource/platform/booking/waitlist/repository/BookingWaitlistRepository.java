
package com.labresource.platform.booking.waitlist.repository;

import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingWaitlistRepository
        extends JpaRepository<BookingWaitlist, Long> {

    @EntityGraph(attributePaths = {"resource", "user"})
    List<BookingWaitlist> findByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"resource", "user"})
    List<BookingWaitlist> findByResourceIdAndStatusOrderByCreatedAtAsc(
            Long resourceId,
            String status
    );

    @EntityGraph(attributePaths = {"resource", "user"})
    List<BookingWaitlist> findByResourceIdAndRequestedStartAndRequestedEndAndStatusOrderByCreatedAtAsc(
            Long resourceId,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            String status
    );

    Optional<BookingWaitlist>
    findFirstByResourceIdAndUserEmailAndRequestedStartAndRequestedEndAndStatusIn(
            Long resourceId,
            String email,
            LocalDateTime start,
            LocalDateTime end,
            List<String> statuses
    );

    @Query("""
        SELECT COUNT(w)
        FROM BookingWaitlist w
        WHERE w.resource.id = :resourceId
          AND w.requestedStart = :start
          AND w.requestedEnd = :end
          AND w.status = 'WAITING'
          AND w.createdAt <= :createdAt
    """)
    long countPosition(
            @Param("resourceId") Long resourceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("createdAt") LocalDateTime createdAt
    );

    /**
     * Fetch active offers that overlap with the maintenance window.
     * Pessimistic locking prevents concurrent changes while processing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w
        FROM BookingWaitlist w
        JOIN FETCH w.user
        JOIN FETCH w.resource
        WHERE w.resource.id = :resourceId
          AND w.status = 'OFFERED'
          AND w.requestedStart < :maintenanceEnd
          AND w.requestedEnd > :maintenanceStart
        ORDER BY w.createdAt ASC
    """)
    List<BookingWaitlist> findOverlappingOffersForMaintenance(
            @Param("resourceId") Long resourceId,
            @Param("maintenanceStart") LocalDateTime maintenanceStart,
            @Param("maintenanceEnd") LocalDateTime maintenanceEnd
    );
    @EntityGraph(attributePaths = {"resource"})
    List<BookingWaitlist> findTop5ByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            List<String> statuses
    );

    long countByUserIdAndStatusIn(
            Long userId,
            List<String> statuses
    );
    @Query("""
        SELECT w.resource.id, COUNT(w)
        FROM BookingWaitlist w
        WHERE w.resource.institution.id = :institutionId
          AND w.createdAt >= :fromDate
          AND w.status IN ('WAITING', 'OFFERED')
          AND (:departmentId IS NULL OR w.resource.department.id = :departmentId)
        GROUP BY w.resource.id
    """)
    List<Object[]> countDashboardWaitlistByResource(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("fromDate") LocalDateTime fromDate
    );
}
