package com.labresource.platform.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import com.labresource.platform.notification.entity.PushDeviceToken;
import com.labresource.platform.notification.repository.PushDeviceTokenRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FirebasePushService {

    private final PushDeviceTokenRepository deviceTokenRepository;
    private final ObjectProvider<FirebaseApp> firebaseAppProvider;

    @Value("${app.firebase.enabled:false}")
    private boolean enabled;

    public FirebasePushService(
            PushDeviceTokenRepository deviceTokenRepository,
            ObjectProvider<FirebaseApp> firebaseAppProvider) {

        this.deviceTokenRepository = deviceTokenRepository;
        this.firebaseAppProvider = firebaseAppProvider;
    }

    public int sendToUser(
            Long userId,
            String title,
            String body,
            Long notificationId) {

        if (!enabled) {
            throw new IllegalStateException(
                    "Firebase push integration is disabled");
        }

        FirebaseApp app = firebaseAppProvider.getIfAvailable();

        if (app == null) {
            throw new IllegalStateException(
                    "Firebase is not initialized");
        }

        List<PushDeviceToken> devices =
                deviceTokenRepository.findByUserIdAndActiveTrue(userId);

        if (devices.isEmpty()) {
            return 0;
        }

        int successCount = 0;

        for (PushDeviceToken device : devices) {

            try {
                Message message = Message.builder()
                        .setToken(device.getDeviceToken())
                        .setNotification(
                                Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                        .putAllData(Map.of(
                                "notificationId",
                                String.valueOf(notificationId),
                                "type",
                                "IN_APP_NOTIFICATION"
                        ))
                        .build();

                FirebaseMessaging.getInstance(app).send(message);
                successCount++;

            } catch (Exception ex) {
                // Keep token active for transient errors.
                // Invalid tokens should be deactivated after
                // classifying the Firebase error code.
                throw new IllegalStateException(
                        "Firebase push delivery failed for device "
                                + device.getId(),
                        ex);
            }
        }

        return successCount;
    }
}