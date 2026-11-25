
package com.labresource.platform.storage;

import com.labresource.platform.maintenance.entity.WorkOrder;
import com.labresource.platform.maintenance.repository.WorkOrderRepository;
import com.labresource.platform.resource.entity.Resource;
import com.labresource.platform.resource.repository.ResourceRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class StoredDocumentService {

    private final StoredDocumentRepository documentRepository;
    private final ResourceRepository resourceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public StoredDocumentService(
            StoredDocumentRepository documentRepository,
            ResourceRepository resourceRepository,
            WorkOrderRepository workOrderRepository,
            UserRepository userRepository,
            S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.resourceRepository = resourceRepository;
        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    @Transactional
    public StoredDocumentResponse upload(
            Long resourceId,
            Long workOrderId,
            String documentType,
            MultipartFile file,
            String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitution() != null
                        && institutionId.equals(r.getInstitution().getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resource not found in your institution"
                ));

        WorkOrder workOrder = null;

        if (workOrderId != null) {
            workOrder = workOrderRepository.findDetailedById(workOrderId)
                    .filter(w -> w.getResource().getInstitution() != null
                            && institutionId.equals(
                            w.getResource().getInstitution().getId()))
                    .filter(w -> w.getResource().getId().equals(resourceId))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Work order not found for this resource"
                    ));
        }

        String normalizedType = normalizeDocumentType(documentType);

        String storageKey = null;

        try {
            storageKey = s3Service.uploadFile(file);

            StoredDocument document = new StoredDocument();
            document.setResource(resource);
            document.setWorkOrder(workOrder);
            document.setUploadedBy(user);
            document.setDocumentType(normalizedType);
            document.setOriginalFileName(file.getOriginalFilename());
            document.setStorageKey(storageKey);
            document.setContentType(detectContentType(file));
            document.setFileSize(file.getSize());

            StoredDocument saved = documentRepository.save(document);

            return toResponse(saved);

        } catch (IOException | RuntimeException exception) {

            if (storageKey != null) {
                try {
                    s3Service.deleteFile(storageKey);
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            if (exception instanceof ResponseStatusException responseException) {
                throw responseException;
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Document upload failed",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<StoredDocumentResponse> getResourceDocuments(
            Long resourceId,
            String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        Resource resource = resourceRepository.findById(resourceId)
                .filter(r -> r.getInstitution() != null
                        && institutionId.equals(r.getInstitution().getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resource not found"
                ));

        return documentRepository
                .findByResourceAndInstitution(resource.getId(), institutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoredDocumentResponse> getWorkOrderDocuments(
            Long workOrderId,
            String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        workOrderRepository.findDetailedById(workOrderId)
                .filter(w -> w.getResource().getInstitution() != null
                        && institutionId.equals(
                        w.getResource().getInstitution().getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Work order not found"
                ));

        return documentRepository
                .findByWorkOrderAndInstitution(workOrderId, institutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getSecureViewLink(Long documentId, String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        StoredDocument document = documentRepository
                .findByIdAndInstitution(documentId, institutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        try {
            return s3Service.generatePresignedUrl(
                    document.getStorageKey()
            );
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate document link",
                    exception
            );
        }
    }

    @Transactional
    public void deleteDocument(Long documentId, String email) {

        User user = getUser(email);
        Long institutionId = getInstitutionId(user);

        StoredDocument document = documentRepository
                .findByIdAndInstitution(documentId, institutionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        String role = user.getRole() == null
                ? ""
                : user.getRole().getName();

        boolean manager = role.equals("LAB_MANAGER")
                || role.equals("INSTITUTION_ADMIN")
                || role.equals("SYSTEM_ADMIN");

        boolean uploader = document.getUploadedBy() != null
                && document.getUploadedBy().getId().equals(user.getId());

        if (!manager && !uploader) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to delete this document"
            );
        }

        try {
            // Delete cloud object first. If this fails, keep DB metadata.
            s3Service.deleteFile(document.getStorageKey());
            documentRepository.delete(document);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Document deletion failed",
                    exception
            );
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    private Long getInstitutionId(User user) {
        if (user.getInstitution() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is not linked to an institution"
            );
        }

        return user.getInstitution().getId();
    }

    private String normalizeDocumentType(String type) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document type is required"
            );
        }

        String value = type.trim().toUpperCase(Locale.ROOT);

        if (!List.of(
                "CALIBRATION_CERTIFICATE",
                "MANUAL",
                "REPORT",
                "OTHER"
        ).contains(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid document type"
            );
        }

        return value;
    }

    private String detectContentType(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();

            if (bytes.length >= 5
                    && bytes[0] == '%'
                    && bytes[1] == 'P'
                    && bytes[2] == 'D'
                    && bytes[3] == 'F'
                    && bytes[4] == '-') {
                return "application/pdf";
            }

            if (bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A) {
                return "image/png";
            }

            if (bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file content"
            );

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded file",
                    exception
            );
        }
    }

    private StoredDocumentResponse toResponse(StoredDocument document) {
        return new StoredDocumentResponse(
                document.getId(),
                document.getResource().getId(),
                document.getWorkOrder() == null
                        ? null
                        : document.getWorkOrder().getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getUploadedBy().getFullName(),
                document.getUploadedAt()
        );
    }
}
