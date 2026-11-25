
package com.labresource.platform.maintenance.repository;

import com.labresource.platform.maintenance.entity.WorkOrderNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderNoteRepository
        extends JpaRepository<WorkOrderNote, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<WorkOrderNote> findByWorkOrderIdOrderByCreatedAtDesc(Long workOrderId);
}
