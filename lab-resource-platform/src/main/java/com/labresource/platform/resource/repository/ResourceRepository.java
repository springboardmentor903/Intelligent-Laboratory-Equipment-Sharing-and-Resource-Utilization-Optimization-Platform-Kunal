
package com.labresource.platform.resource.repository;

import com.labresource.platform.resource.entity.Resource;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByCategory(String category);

    List<Resource> findByStatus(String status);

    List<Resource> findByLocationContainingIgnoreCase(String location);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :resourceId")
    Optional<Resource> findByIdForUpdate(
            @Param("resourceId") Long resourceId
    );

    @EntityGraph(attributePaths = {
            "institution",
            "department",
            "tags"
    })
    List<Resource> findAll();

    @EntityGraph(attributePaths = {
            "institution",
            "department",
            "tags"
    })
    Optional<Resource> findById(Long id);

    @Query("""
            SELECT COUNT(r)
            FROM Resource r
            WHERE r.institution.id = :institutionId
            AND r.isActive = true
            """)
    long countActiveResourcesByInstitutionId(
            @Param("institutionId") Long institutionId
    );
    @Query("""
        SELECT r
        FROM Resource r
        WHERE r.institution.id = :institutionId
          AND r.category IN :categories
          AND r.isActive = true
          AND r.availableQuantity > 0
          AND r.status = 'AVAILABLE'
          AND r.id NOT IN :excludedIds
        """)
    List<Resource> findRecommendationCandidates(
            @Param("institutionId") Long institutionId,
            @Param("categories") List<String> categories,
            @Param("excludedIds") List<Long> excludedIds
    );
}
