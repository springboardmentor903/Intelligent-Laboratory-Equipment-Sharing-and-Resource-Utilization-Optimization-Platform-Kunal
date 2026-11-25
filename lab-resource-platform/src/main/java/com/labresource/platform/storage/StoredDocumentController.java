
package com.labresource.platform.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class StoredDocumentController {

    private final StoredDocumentService documentService;

    public StoredDocumentController(
            StoredDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<StoredDocumentResponse> upload(
            @RequestParam Long resourceId,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        StoredDocumentResponse response = documentService.upload(
                resourceId,
                workOrderId,
                documentType,
                file,
                authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<StoredDocumentResponse>> getResourceDocuments(
            @PathVariable Long resourceId,
            Authentication authentication) {

        return ResponseEntity.ok(
                documentService.getResourceDocuments(
                        resourceId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/work-order/{workOrderId}")
    public ResponseEntity<List<StoredDocumentResponse>> getWorkOrderDocuments(
            @PathVariable Long workOrderId,
            Authentication authentication) {

        return ResponseEntity.ok(
                documentService.getWorkOrderDocuments(
                        workOrderId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{documentId}/view-link")
    public ResponseEntity<Map<String, String>> getViewLink(
            @PathVariable Long documentId,
            Authentication authentication) {

        String url = documentService.getSecureViewLink(
                documentId,
                authentication.getName()
        );

        return ResponseEntity.ok(Map.of(
                "url", url,
                "expiresInMinutes", "10"
        ));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) {

        documentService.deleteDocument(
                documentId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
