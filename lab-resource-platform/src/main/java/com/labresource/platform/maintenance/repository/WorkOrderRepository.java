
package com.labresource.platform.maintenance.repository;

import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.enums.WorkOrderStatus;
import com.labresource.platform.maintenance.enums.WorkOrderType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsByWorkOrderNumber(String workOrderNumber);

    boolean existsByResourceIdAndStatusIn(
            Long resourceId,
            Collection<WorkOrderStatus> statuses
    );

    boolean existsByResourceIdAndTypeAndStatusIn(
            Long resourceId,
            WorkOrderType type,
            Collection<WorkOrderStatus> statuses
    );

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "assignedTechnician",
            "createdBy"
    })
    @Query("SELECT w FROM WorkOrder w WHERE w.id = :id")
    Optional<WorkOrder> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "assignedTechnician"
    })
    @Query("""
        SELECT w FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND (:status IS NULL OR w.status = :status)
          AND (:type IS NULL OR w.type = :type)
          AND (:resourceId IS NULL OR w.resource.id = :resourceId)
    """)
    Page<WorkOrder> findInstitutionWorkOrders(
            @Param("institutionId") Long institutionId,
            @Param("status") WorkOrderStatus status,
            @Param("type") WorkOrderType type,
            @Param("resourceId") Long resourceId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "assignedTechnician"
    })
    Page<WorkOrder> findByAssignedTechnicianId(
            Long technicianId,
            Pageable pageable
    );

    List<WorkOrder> findByResourceIdAndStatusIn(
            Long resourceId,
            Collection<WorkOrderStatus> statuses
    );

    long countByResourceInstitutionIdAndStatusIn(
            Long institutionId,
            Collection<WorkOrderStatus> statuses
    );

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "assignedTechnician"
    })
    @Query("""
        SELECT w FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND w.assignedTechnician.id = :technicianId
          AND (:status IS NULL OR w.status = :status)
          AND (:departmentId IS NULL OR w.resource.department.id = :departmentId)
          AND (
                :keyword = ''
                OR LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(w.workOrderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(w.resource.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<WorkOrder> findTechnicianTasks(
            @Param("institutionId") Long institutionId,
            @Param("technicianId") Long technicianId,
            @Param("status") WorkOrderStatus status,
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department",
            "assignedTechnician"
    })
    @Query("""
        SELECT w FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND w.assignedTechnician.id = :technicianId
          AND w.status IN :statuses
        ORDER BY
          CASE WHEN w.scheduledEnd IS NULL THEN w.expectedEnd
               ELSE w.scheduledEnd END ASC
    """)
    List<WorkOrder> findTechnicianActiveTasks(
            @Param("institutionId") Long institutionId,
            @Param("technicianId") Long technicianId,
            @Param("statuses") Collection<WorkOrderStatus> statuses
    );

    long countByAssignedTechnicianIdAndStatusIn(
            Long technicianId,
            Collection<WorkOrderStatus> statuses
    );

    long countByAssignedTechnicianIdAndStatus(
            Long technicianId,
            WorkOrderStatus status
    );

    @Query("""
        SELECT COUNT(w) FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND w.assignedTechnician.id = :technicianId
          AND w.status IN :statuses
          AND (
              (w.scheduledEnd IS NOT NULL AND w.scheduledEnd < :now)
              OR
              (w.scheduledEnd IS NULL AND w.expectedEnd IS NOT NULL
               AND w.expectedEnd < :now)
          )
    """)
    long countOverdueTechnicianTasks(
            @Param("institutionId") Long institutionId,
            @Param("technicianId") Long technicianId,
            @Param("statuses") Collection<WorkOrderStatus> statuses,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT w FROM WorkOrder w
        WHERE w.preventiveSchedule.id = :scheduleId
          AND w.type = :type
          AND w.status = :status
    """)
    List<WorkOrder> findPreventiveWorkOrdersByScheduleAndStatus(
            @Param("scheduleId") Long scheduleId,
            @Param("type") WorkOrderType type,
            @Param("status") WorkOrderStatus status
    );

    // Session 3 - Completed work orders with maintenance cost
    @EntityGraph(attributePaths = {
            "resource",
            "resource.institution",
            "resource.department"
    })
    @Query("""
        SELECT w FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND w.actualCost IS NOT NULL
          AND w.completedAt IS NOT NULL
          AND w.completedAt >= :fromDate
          AND w.completedAt < :toDate
    """)
    List<WorkOrder> findCostedCompletedWorkOrders(
            @Param("institutionId") Long institutionId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
    @Query("""
        SELECT w.status, COUNT(w)
        FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND (:departmentId IS NULL OR w.resource.department.id = :departmentId)
          AND w.status <> com.labresource.platform.maintenance.enums.WorkOrderStatus.COMPLETED
          AND w.status <> com.labresource.platform.maintenance.enums.WorkOrderStatus.CANCELLED
        GROUP BY w.status
    """)
    List<Object[]> countOpenDashboardWorkOrders(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId
    );

    @EntityGraph(attributePaths = {"resource", "resource.department"})
    @Query("""
        SELECT w FROM WorkOrder w
        WHERE w.resource.institution.id = :institutionId
          AND w.status IN (
              com.labresource.platform.maintenance.enums.WorkOrderStatus.OPEN,
              com.labresource.platform.maintenance.enums.WorkOrderStatus.ASSIGNED,
              com.labresource.platform.maintenance.enums.WorkOrderStatus.IN_PROGRESS
          )
          AND (:departmentId IS NULL OR w.resource.department.id = :departmentId)
        ORDER BY w.createdAt DESC
    """)
    List<WorkOrder> findDashboardActiveWorkOrders(
            @Param("institutionId") Long institutionId,
            @Param("departmentId") Long departmentId
    );
}
