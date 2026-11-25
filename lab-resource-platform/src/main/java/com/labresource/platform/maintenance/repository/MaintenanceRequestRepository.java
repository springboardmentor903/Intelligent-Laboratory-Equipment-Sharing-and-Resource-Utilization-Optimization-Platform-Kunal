
package com.labresource.platform.maintenance.repository;

import com.labresource.platform.maintenance.entity.MaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequest, Long> {

    // Existing compatibility methods
    List<MaintenanceRequest> findByStatus(String status);

    List<MaintenanceRequest> findByResourceId(Long resourceId);

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "requester",
            "reviewedBy"
    })
    @Query("""
        SELECT m
        FROM MaintenanceRequest m
        WHERE m.id = :id
        """)
    Optional<MaintenanceRequest> findByIdWithResource(
            @Param("id") Long id
    );

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "requester",
            "reviewedBy"
    })
    @Query("""
        SELECT m
        FROM MaintenanceRequest m
        ORDER BY m.id DESC
        """)
    List<MaintenanceRequest> findAllWithResource();

    // Personal maintenance requests with filters and pagination
    @Query("""
        SELECT m
        FROM MaintenanceRequest m
        WHERE m.requester.id = :userId
        AND (:status IS NULL OR m.status = :status)
        AND (:resourceId IS NULL OR m.resource.id = :resourceId)
        AND (:priority IS NULL OR m.priority = :priority)
        """)
    Page<MaintenanceRequest> findMyRequests(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("resourceId") Long resourceId,
            @Param("priority") String priority,
            Pageable pageable
    );

    // Institution-specific manager request queue
    @Query("""
        SELECT m
        FROM MaintenanceRequest m
        WHERE m.resource.institution.id = :institutionId
        AND (:status IS NULL OR m.status = :status)
        AND (:resourceId IS NULL OR m.resource.id = :resourceId)
        AND (:priority IS NULL OR m.priority = :priority)
        """)
    Page<MaintenanceRequest> findInstitutionRequests(
            @Param("institutionId") Long institutionId,
            @Param("status") String status,
            @Param("resourceId") Long resourceId,
            @Param("priority") String priority,
            Pageable pageable
    );

    // Requests belonging to a particular equipment
    Page<MaintenanceRequest> findByResourceId(
            Long resourceId,
            Pageable pageable
    );

    // Count pending requests for an institution
    long countByResourceInstitutionIdAndStatus(
            Long institutionId,
            String status
    );
}
