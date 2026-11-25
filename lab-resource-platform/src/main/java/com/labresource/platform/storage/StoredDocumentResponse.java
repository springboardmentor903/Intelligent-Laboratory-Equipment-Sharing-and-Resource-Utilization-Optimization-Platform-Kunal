
package com.labresource.platform.storage;

import java.time.LocalDateTime;

public record StoredDocumentResponse(
        Long id,
        Long resourceId,
        Long workOrderId,
        String documentType,
        String originalFileName,
        String contentType,
        Long fileSize,
        String uploadedBy,
        LocalDateTime uploadedAt
) {
}
