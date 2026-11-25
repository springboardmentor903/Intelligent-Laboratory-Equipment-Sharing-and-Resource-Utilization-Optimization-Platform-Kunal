package com.labresource.platform.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.enabled:false}")
    private boolean enabled;

    @Value("${app.firebase.project-id:}")
    private String projectId;

    @Value("${app.firebase.service-account:}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {

        if (!enabled) {
            return null;
        }

        if (FirebaseApp.getApps().size() > 0) {
            return FirebaseApp.getInstance();
        }

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "Firebase project ID is missing");
        }

        if (serviceAccountPath == null
                || serviceAccountPath.isBlank()) {
            throw new IllegalStateException(
                    "Firebase service account path is missing");
        }

        Path path = Path.of(serviceAccountPath);

        if (!Files.exists(path)) {
            throw new IllegalStateException(
                    "Firebase service account file not found");
        }

        try (InputStream input = new FileInputStream(path.toFile())) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials.fromStream(input)
                                    .createScoped(List.of(
                                            "https://www.googleapis.com/auth/firebase.messaging"
                                    )))
                    .setProjectId(projectId)
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }
}