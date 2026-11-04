
package com.labresource.platform.sharing.repository;

import com.labresource.platform.sharing.entity.ExternalBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExternalBookingRepository
        extends JpaRepository<ExternalBooking, Long> {

    @Query("""
        SELECT COUNT(eb) FROM ExternalBooking eb
        WHERE eb.resourceShare.resource.id = :resourceId
        AND eb.status IN ('PENDING', 'APPROVED')
        AND eb.startTime < :endTime
        AND eb.endTime > :startTime
    """)
    long countOverlappingExternalBookings(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT eb FROM ExternalBooking eb
        JOIN FETCH eb.resourceShare rs
        JOIN FETCH rs.resource
        JOIN FETCH eb.requestedBy
        WHERE eb.requestedBy.id = :userId
        ORDER BY eb.createdAt DESC
    """)
    List<ExternalBooking> findForRequester(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT eb FROM ExternalBooking eb
        JOIN FETCH eb.resourceShare rs
        JOIN FETCH rs.resource
        JOIN FETCH eb.requestedBy
        WHERE rs.sharedByInstitutionId = :institutionId
        ORDER BY eb.createdAt DESC
    """)
    List<ExternalBooking> findForResourceOwner(
            @Param("institutionId") Long institutionId
    );

    @Query("""
        SELECT eb FROM ExternalBooking eb
        JOIN FETCH eb.resourceShare rs
        JOIN FETCH rs.resource
        JOIN FETCH eb.requestedBy
        WHERE rs.sharedWithInstitutionId = :institutionId
        ORDER BY eb.createdAt DESC
    """)
    List<ExternalBooking> findForRequestingInstitution(
            @Param("institutionId") Long institutionId
    );

    @Query("""
        SELECT eb FROM ExternalBooking eb
        JOIN FETCH eb.resourceShare rs
        JOIN FETCH rs.resource
        JOIN FETCH eb.requestedBy
        WHERE eb.id = :id
    """)
    Optional<ExternalBooking> findDetailsById(
            @Param("id") Long id
    );
}
