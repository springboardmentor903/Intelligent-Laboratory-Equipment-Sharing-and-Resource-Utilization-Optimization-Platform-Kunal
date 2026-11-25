
package com.labresource.platform.maintenance.repository;

import com.labresource.platform.maintenance.entity.WorkOrderServiceLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderServiceLogRepository
        extends JpaRepository<WorkOrderServiceLog, Long> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<WorkOrderServiceLog> findByWorkOrderIdOrderByChangedAtDesc(Long workOrderId);
}
