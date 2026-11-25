
package com.labresource.platform.maintenance.repository;

import com.labresource.platform.maintenance.entity.PreventiveMaintenanceSchedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PreventiveMaintenanceScheduleRepository
        extends JpaRepository<PreventiveMaintenanceSchedule, Long> {

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "createdBy"
    })
    List<PreventiveMaintenanceSchedule>
    findByResourceIdOrderByNextDueAtAsc(Long resourceId);

    // Get ALL schedules (active + inactive) for an institution
    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "createdBy"
    })
    @Query("""
        SELECT s
        FROM PreventiveMaintenanceSchedule s
        WHERE s.resource.institution.id = :institutionId
        ORDER BY s.nextDueAt ASC
    """)
    List<PreventiveMaintenanceSchedule> findAllSchedulesByInstitution(
            @Param("institutionId") Long institutionId
    );

    // Existing active schedules query (kept for other services)
    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "createdBy"
    })
    @Query("""
        SELECT s
        FROM PreventiveMaintenanceSchedule s
        WHERE s.resource.institution.id = :institutionId
          AND s.active = true
        ORDER BY s.nextDueAt ASC
    """)
    List<PreventiveMaintenanceSchedule> findActiveSchedulesByInstitution(
            @Param("institutionId") Long institutionId
    );

    // Find active schedules due within a given date range
    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "createdBy"
    })
    @Query("""
        SELECT s
        FROM PreventiveMaintenanceSchedule s
        WHERE s.active = true
          AND s.nextDueAt <= :dueBefore
        ORDER BY s.nextDueAt ASC
    """)
    List<PreventiveMaintenanceSchedule> findActiveSchedulesDueBefore(
            @Param("dueBefore") LocalDateTime dueBefore
    );

    // Find a schedule with its equipment details
    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "createdBy"
    })
    Optional<PreventiveMaintenanceSchedule> findWithDetailsById(Long id);

    // Check duplicate active schedules
    boolean existsByResourceIdAndTitleIgnoreCaseAndActiveTrue(
            Long resourceId,
            String title
    );
    @EntityGraph(attributePaths = {"resource", "resource.department"})
    @Query("""
        SELECT s
        FROM PreventiveMaintenanceSchedule s
        WHERE s.resource.institution.id = :institutionId
          AND s.active = true
          AND s.nextDueAt >= :today
          AND s.nextDueAt <= :dueLimit
          AND (:departmentId IS NULL OR s.resource.department.id = :departmentId)
        ORDER BY s.nextDueAt ASC
    """)
    List<PreventiveMaintenanceSchedule> findDashboardSchedulesDueSoon(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("today") LocalDateTime today,
            @Param("dueLimit") LocalDateTime dueLimit
    );

    @EntityGraph(attributePaths = {"resource", "resource.department"})
    @Query("""
        SELECT s
        FROM PreventiveMaintenanceSchedule s
        WHERE s.resource.institution.id = :institutionId
          AND s.active = true
          AND s.nextDueAt < :today
          AND (:departmentId IS NULL OR s.resource.department.id = :departmentId)
        ORDER BY s.nextDueAt ASC
    """)
    List<PreventiveMaintenanceSchedule> findDashboardOverdueSchedules(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId,
            @Param("today") LocalDateTime today
    );
}
