
package com.labresource.platform.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        String key = s3Service.uploadFile(file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "File uploaded successfully",
                        "key", key
                ));
    }
}
