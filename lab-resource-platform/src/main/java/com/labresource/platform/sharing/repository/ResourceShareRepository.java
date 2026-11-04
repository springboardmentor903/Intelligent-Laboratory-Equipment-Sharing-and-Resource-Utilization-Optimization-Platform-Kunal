
package com.labresource.platform.sharing.repository;

import com.labresource.platform.sharing.entity.ResourceShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceShareRepository
        extends JpaRepository<ResourceShare, Long> {

    List<ResourceShare> findByStatus(String status);

    List<ResourceShare> findBySharedByUserId(Long userId);

    List<ResourceShare> findBySharedWithUserId(Long userId);

    List<ResourceShare> findByResourceId(Long resourceId);

    List<ResourceShare>
    findBySharedByInstitutionIdOrderByCreatedAtDesc(Long institutionId);

    List<ResourceShare>
    findBySharedWithInstitutionIdOrderByCreatedAtDesc(Long institutionId);

    // Received requests with complete resource details
    @Query("""
        SELECT DISTINCT s
        FROM ResourceShare s
        JOIN FETCH s.resource r
        LEFT JOIN FETCH r.department
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.tags
        WHERE s.sharedWithInstitutionId = :institutionId
        ORDER BY s.createdAt DESC
        """)
    List<ResourceShare> findReceivedRequestsWithResource(
            @Param("institutionId") Long institutionId
    );

    // Sent requests with complete resource details
    @Query("""
        SELECT DISTINCT s
        FROM ResourceShare s
        JOIN FETCH s.resource r
        LEFT JOIN FETCH r.department
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.tags
        WHERE s.sharedByInstitutionId = :institutionId
        ORDER BY s.createdAt DESC
        """)
    List<ResourceShare> findSentRequestsWithResource(
            @Param("institutionId") Long institutionId
    );

    List<ResourceShare>
    findBySharedWithInstitutionIdAndStatusOrderByCreatedAtDesc(
            Long institutionId,
            String status
    );

    List<ResourceShare>
    findBySharedByInstitutionIdAndStatusOrderByCreatedAtDesc(
            Long institutionId,
            String status
    );

    boolean existsByResourceIdAndSharedWithInstitutionIdAndStatus(
            Long resourceId,
            Long institutionId,
            String status
    );

    // All sharing requests with complete resource details
    @Query("""
        SELECT DISTINCT s
        FROM ResourceShare s
        JOIN FETCH s.resource r
        LEFT JOIN FETCH r.department
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.tags
        ORDER BY s.id DESC
        """)
    List<ResourceShare> findAllWithResource();

    // Sharing request by ID with complete resource details
    @Query("""
        SELECT DISTINCT s
        FROM ResourceShare s
        JOIN FETCH s.resource r
        LEFT JOIN FETCH r.department
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.tags
        WHERE s.id = :id
        """)
    Optional<ResourceShare> findByIdWithResource(
            @Param("id") Long id
    );

    // Approved shares received by an institution
    @Query("""
        SELECT DISTINCT s
        FROM ResourceShare s
        JOIN FETCH s.resource r
        LEFT JOIN FETCH r.department
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.tags
        WHERE s.sharedWithInstitutionId = :institutionId
        AND s.status = 'APPROVED'
        ORDER BY s.createdAt DESC
        """)
    List<ResourceShare> findApprovedSharesForInstitution(
            @Param("institutionId") Long institutionId
    );

    // Approved shares sent by an institution
    @Query("""
        SELECT DISTINCT s
        FROM ResourceShare s
        JOIN FETCH s.resource r
        LEFT JOIN FETCH r.department
        LEFT JOIN FETCH r.institution
        LEFT JOIN FETCH r.tags
        WHERE s.sharedByInstitutionId = :institutionId
        AND s.status = 'APPROVED'
        ORDER BY s.createdAt DESC
        """)
    List<ResourceShare> findApprovedSharesByInstitution(
            @Param("institutionId") Long institutionId
    );
}
