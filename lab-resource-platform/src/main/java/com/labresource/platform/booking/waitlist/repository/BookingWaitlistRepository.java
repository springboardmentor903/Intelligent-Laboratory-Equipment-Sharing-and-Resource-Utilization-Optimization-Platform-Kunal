
package com.labresource.platform.booking.waitlist.repository;

import com.labresource.platform.booking.waitlist.entity.BookingWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

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
}
