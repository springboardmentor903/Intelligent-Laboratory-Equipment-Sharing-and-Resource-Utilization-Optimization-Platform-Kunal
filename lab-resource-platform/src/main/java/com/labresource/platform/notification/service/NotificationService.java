
package com.labresource.platform.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labresource.platform.notification.dto.NotificationEventMessage;
import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.notification.repository.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private static final String NOTIFICATION_TOPIC = "lab-events";

    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRealtimePublisher realtimePublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            NotificationRealtimePublisher realtimePublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(
            Long userId,
            Boolean unread,
            String type,
            Pageable pageable
    ) {
        String normalizedType =
                type == null || type.isBlank()
                        ? null
                        : type.trim().toUpperCase(Locale.ROOT);

        return notificationRepository.findFilteredNotifications(
                userId,
                unread,
                normalizedType,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getLatestNotifications(Long userId) {
        return notificationRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(
                userId,
                false
        );
    }

    @Transactional
    public Notification createNotification(
            Long userId,
            String title,
            String message,
            String type,
            String referenceType,
            Long referenceId
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid user ID is required");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());

        notification.setType(
                type == null || type.isBlank()
                        ? "GENERAL"
                        : type.trim().toUpperCase(Locale.ROOT)
        );

        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);

        registerAfterCommit(saved.getId(), userId);

        return saved;
    }

    @Transactional
    public Notification createNotification(
            Long userId,
            String title,
            String message,
            String type
    ) {
        return createNotification(
                userId,
                title,
                message,
                type,
                null,
                null
        );
    }

    @Transactional
    public Notification markAsRead(
            Long notificationId,
            Long userId
    ) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Notification not found")
                );

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());

            notificationRepository.save(notification);

            registerUnreadCountAfterCommit(userId);
        }

        return notification;
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(
                userId,
                LocalDateTime.now()
        );

        if (updated > 0) {
            registerUnreadCountAfterCommit(userId);
        }

        return updated;
    }

    private void registerAfterCommit(
            Long notificationId,
            Long userId
    ) {
        Runnable action = () -> {
            publishKafkaEvent(notificationId, userId);
            publishUnreadCountSafely(userId);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
        } else {
            action.run();
        }
    }

    private void registerUnreadCountAfterCommit(Long userId) {
        Runnable action = () -> publishUnreadCountSafely(userId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
        } else {
            action.run();
        }
    }

    private void publishKafkaEvent(
            Long notificationId,
            Long userId
    ) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new NotificationEventMessage(notificationId, userId)
            );

            kafkaTemplate.send(
                    NOTIFICATION_TOPIC,
                    String.valueOf(notificationId),
                    payload
            ).whenComplete((result, error) -> {
                if (error != null) {
                    log.error(
                            "Failed to publish notification event {}",
                            notificationId,
                            error
                    );
                }
            });

        } catch (JsonProcessingException ex) {
            log.error(
                    "Could not serialize notification event {}",
                    notificationId,
                    ex
            );
        }
    }

    private void publishUnreadCountSafely(Long userId) {
        try {
            long unreadCount = notificationRepository
                    .countByUserIdAndIsRead(userId, false);

            realtimePublisher.publishUnreadCount(
                    userId,
                    unreadCount
            );

        } catch (Exception ex) {
            log.warn(
                    "Could not publish unread count for user {}",
                    userId,
                    ex
            );
        }
    }
}
