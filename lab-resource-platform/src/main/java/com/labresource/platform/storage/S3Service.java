
package com.labresource.platform.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
public class S3Service {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String uploadFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a non-empty file."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB."
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException(
                    "File name is required."
            );
        }

        String extension = getExtension(originalName);
        byte[] content = file.getBytes();

        String detectedContentType = detectContentType(content);

        validateExtensionAndContent(
                extension,
                detectedContentType
        );

        String safeName = originalName.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );

        String key = "lab-documents/"
                + UUID.randomUUID()
                + "-"
                + safeName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(detectedContentType)
                .contentLength((long) content.length)
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(content)
        );

        return key;
    }

    public void deleteFile(String key) {

        if (key == null || key.isBlank()
                || !key.startsWith("lab-documents/")) {
            throw new IllegalArgumentException(
                    "Invalid storage file reference."
            );
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()
        );
    }

    public String generatePresignedUrl(String key) {

        if (key == null || key.isBlank()
                || !key.startsWith("lab-documents/")) {
            throw new IllegalArgumentException(
                    "Invalid storage file reference."
            );
        }

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    private String getExtension(String fileName) {

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException(
                    "Only PDF, PNG and JPEG files are allowed."
            );
        }

        return fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String detectContentType(byte[] content) {

        // PDF signature: %PDF-
        if (content.length >= 5
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-') {
            return "application/pdf";
        }

        // PNG signature
        if (content.length >= 8
                && (content[0] & 0xFF) == 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47
                && content[4] == 0x0D
                && content[5] == 0x0A
                && content[6] == 0x1A
                && content[7] == 0x0A) {
            return "image/png";
        }

        // JPEG signature
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        throw new IllegalArgumentException(
                "Invalid file content. Only genuine PDF, PNG and JPEG files are allowed."
        );
    }

    private void validateExtensionAndContent(
            String extension,
            String detectedContentType) {

        boolean valid =
                ("pdf".equals(extension)
                        && "application/pdf".equals(detectedContentType))
                        || ("png".equals(extension)
                        && "image/png".equals(detectedContentType))
                        || (("jpg".equals(extension) || "jpeg".equals(extension))
                        && "image/jpeg".equals(detectedContentType));

        if (!valid) {
            throw new IllegalArgumentException(
                    "File extension does not match its actual content."
            );
        }
    }
}
