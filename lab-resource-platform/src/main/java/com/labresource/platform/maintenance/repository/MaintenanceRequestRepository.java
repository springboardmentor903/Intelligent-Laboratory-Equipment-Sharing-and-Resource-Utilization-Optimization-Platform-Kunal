
package com.labresource.platform.maintenance.repository;

import com.labresource.platform.maintenance.entity.MaintenanceRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByStatus(String status);

    List<MaintenanceRequest> findByResourceId(Long resourceId);

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "resource.tags"
    })
    @Query("""
        SELECT DISTINCT m
        FROM MaintenanceRequest m
        JOIN FETCH m.resource
        ORDER BY m.id
        """)
    List<MaintenanceRequest> findAllWithResource();

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "resource.tags"
    })
    @Query("""
        SELECT m
        FROM MaintenanceRequest m
        JOIN FETCH m.resource
        WHERE m.id = :id
        """)
    Optional<MaintenanceRequest> findByIdWithResource(@Param("id") Long id);
}
