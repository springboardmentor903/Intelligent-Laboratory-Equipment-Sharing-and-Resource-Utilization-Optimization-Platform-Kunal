
package com.labresource.platform.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoredDocumentRepository
        extends JpaRepository<StoredDocument, Long> {

    @Query("""
        SELECT d
        FROM StoredDocument d
        JOIN FETCH d.resource r
        LEFT JOIN FETCH d.workOrder w
        WHERE d.id = :documentId
        AND r.institution.id = :institutionId
    """)
    Optional<StoredDocument> findByIdAndInstitution(
            @Param("documentId") Long documentId,
            @Param("institutionId") Long institutionId
    );

    @Query("""
        SELECT d
        FROM StoredDocument d
        JOIN FETCH d.resource r
        LEFT JOIN FETCH d.workOrder w
        WHERE r.id = :resourceId
        AND r.institution.id = :institutionId
        ORDER BY d.uploadedAt DESC
    """)
    List<StoredDocument> findByResourceAndInstitution(
            @Param("resourceId") Long resourceId,
            @Param("institutionId") Long institutionId
    );

    @Query("""
        SELECT d
        FROM StoredDocument d
        JOIN FETCH d.resource r
        LEFT JOIN FETCH d.workOrder w
        WHERE w.id = :workOrderId
        AND r.institution.id = :institutionId
        ORDER BY d.uploadedAt DESC
    """)
    List<StoredDocument> findByWorkOrderAndInstitution(
            @Param("workOrderId") Long workOrderId,
            @Param("institutionId") Long institutionId
    );
}
