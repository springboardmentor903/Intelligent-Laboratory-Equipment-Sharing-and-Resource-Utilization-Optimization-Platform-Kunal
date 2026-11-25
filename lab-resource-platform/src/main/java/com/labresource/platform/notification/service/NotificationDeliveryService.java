
package com.labresource.platform.notification.service;

import com.labresource.platform.notification.entity.*;
import com.labresource.platform.notification.repository.*;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class NotificationDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationDeliveryService.class);

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PushDeviceTokenRepository deviceRepository;
    private final UserRepository userRepository;
    private final NotificationEmailService emailService;
    private final Msg91SmsService smsService;
    private final FirebasePushService pushService;

    public NotificationDeliveryService(
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationPreferenceRepository preferenceRepository,
            PushDeviceTokenRepository deviceRepository,
            UserRepository userRepository,
            NotificationEmailService emailService,
            Msg91SmsService smsService,
            FirebasePushService pushService
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.preferenceRepository = preferenceRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.pushService = pushService;
    }

    @Transactional
    public void processNotification(Long notificationId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId
                ));

        Long userId = notification.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification recipient not found: " + userId
                ));

        NotificationPreference preference = getPreference(userId);

        processEmail(notification, user, preference);
        processSms(notification, user, preference);
        processPush(notification, userId, preference);
    }

    /**
     * Retries only the specific failed delivery.
     * It does not reprocess other channels of the notification.
     */
    @Transactional
    public void retryDelivery(Long deliveryId) {

        NotificationDelivery delivery = deliveryRepository
                .findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Delivery not found: " + deliveryId
                ));

        if (delivery.getStatus() != NotificationDeliveryStatus.FAILED) {
            return;
        }

        int attempts = delivery.getAttemptCount() == null
                ? 0
                : delivery.getAttemptCount();

        if (attempts >= MAX_ATTEMPTS) {
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);

            log.warn(
                    "Retry limit reached. Delivery ID={}, channel={}, attempts={}",
                    deliveryId,
                    delivery.getChannel(),
                    attempts
            );
            return;
        }

        Notification notification = delivery.getNotification();

        if (notification == null) {
            log.error("Notification missing for delivery ID={}", deliveryId);
            return;
        }

        Long userId = notification.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification recipient not found: " + userId
                ));

        NotificationPreference preference = getPreference(userId);

        String channel = delivery.getChannel() == null
                ? ""
                : delivery.getChannel().toUpperCase(Locale.ROOT);

        switch (channel) {
            case "EMAIL":
                processEmail(notification, user, preference);
                break;

            case "SMS":
                processSms(notification, user, preference);
                break;

            case "PUSH":
                processPush(notification, userId, preference);
                break;

            default:
                log.warn(
                        "Unknown delivery channel '{}' for delivery ID={}",
                        channel,
                        deliveryId
                );
        }
    }

    private NotificationPreference getPreference(Long userId) {

        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference defaults =
                            new NotificationPreference();

                    defaults.setUserId(userId);

                    return preferenceRepository.save(defaults);
                });
    }

    private void processEmail(
            Notification notification,
            User user,
            NotificationPreference preference
    ) {
        NotificationDelivery delivery =
                getOrCreateDelivery(notification, "EMAIL");

        if (isFinal(delivery)) return;

        if (isRetryLimitReached(delivery)) return;

        if (!Boolean.TRUE.equals(preference.getEmailEnabled())) {
            markSkipped(delivery, "Email notifications disabled");
            return;
        }

        try {
            smsSafeIncrementNotNeeded(delivery);
            emailService.sendNotificationEmail(user, notification);
            markSent(delivery);

        } catch (Exception ex) {
            markFailed(delivery, ex);
        }
    }

    private void processSms(
            Notification notification,
            User user,
            NotificationPreference preference
    ) {
        NotificationDelivery delivery =
                getOrCreateDelivery(notification, "SMS");

        if (isFinal(delivery)) return;

        if (isRetryLimitReached(delivery)) return;

        if (!Boolean.TRUE.equals(preference.getSmsEnabled())) {
            markSkipped(delivery, "SMS notifications disabled");
            return;
        }

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            markSkipped(delivery, "Recipient phone number unavailable");
            return;
        }

        try {
            smsService.sendSms(
                    user.getPhone(),
                    notification.getTitle(),
                    notification.getMessage()
            );

            markSent(delivery);

        } catch (Exception ex) {
            markFailed(delivery, ex);
        }
    }

    private void processPush(
            Notification notification,
            Long userId,
            NotificationPreference preference
    ) {
        NotificationDelivery delivery =
                getOrCreateDelivery(notification, "PUSH");

        if (isFinal(delivery)) return;

        if (isRetryLimitReached(delivery)) return;

        if (!Boolean.TRUE.equals(preference.getPushEnabled())) {
            markSkipped(delivery, "Push notifications disabled");
            return;
        }

        if (!deviceRepository.existsByUserIdAndActiveTrue(userId)) {
            markSkipped(delivery, "No active push device registered");
            return;
        }

        try {
            int delivered = pushService.sendToUser(
                    userId,
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getId()
            );

            if (delivered <= 0) {
                markSkipped(
                        delivery,
                        "No active push device accepted the notification"
                );
                return;
            }

            markSent(delivery);

        } catch (Exception ex) {
            markFailed(delivery, ex);
        }
    }

    private NotificationDelivery getOrCreateDelivery(
            Notification notification,
            String channel
    ) {
        return deliveryRepository
                .findByNotificationIdAndChannel(
                        notification.getId(),
                        channel
                )
                .orElseGet(() -> {
                    NotificationDelivery delivery =
                            new NotificationDelivery();

                    delivery.setNotification(notification);
                    delivery.setChannel(channel);
                    delivery.setStatus(
                            NotificationDeliveryStatus.PENDING
                    );
                    delivery.setAttemptCount(0);

                    return deliveryRepository.save(delivery);
                });
    }

    private boolean isFinal(NotificationDelivery delivery) {
        return delivery.getStatus() == NotificationDeliveryStatus.SENT
                || delivery.getStatus() == NotificationDeliveryStatus.SKIPPED;
    }

    private boolean isRetryLimitReached(NotificationDelivery delivery) {

        int attempts = delivery.getAttemptCount() == null
                ? 0
                : delivery.getAttemptCount();

        if (attempts >= MAX_ATTEMPTS) {
            delivery.setNextRetryAt(null);
            return true;
        }

        return false;
    }

    private void markSent(NotificationDelivery delivery) {
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setSentAt(LocalDateTime.now());
        delivery.setLastError(null);
        delivery.setNextRetryAt(null);
    }

    private void markSkipped(
            NotificationDelivery delivery,
            String reason
    ) {
        delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
        delivery.setLastError(reason);
        delivery.setNextRetryAt(null);
    }

    private void markFailed(
            NotificationDelivery delivery,
            Exception ex
    ) {
        int attempts = delivery.getAttemptCount() == null
                ? 0
                : delivery.getAttemptCount();

        attempts++;

        delivery.setAttemptCount(attempts);
        delivery.setStatus(NotificationDeliveryStatus.FAILED);
        delivery.setLastError(
                ex.getMessage() == null
                        ? "Delivery failed"
                        : ex.getMessage()
        );

        if (attempts < MAX_ATTEMPTS) {
            delivery.setNextRetryAt(
                    LocalDateTime.now().plusMinutes(5)
            );
        } else {
            delivery.setNextRetryAt(null);
        }

        log.error(
                "Delivery failed: notification={}, channel={}, attempt={}",
                delivery.getNotification().getId(),
                delivery.getChannel(),
                attempts,
                ex
        );
    }

    // Kept as a no-op to avoid changing the existing email flow.
    private void smsSafeIncrementNotNeeded(NotificationDelivery delivery) {
        // Attempt count is incremented only when delivery fails.
    }
}
