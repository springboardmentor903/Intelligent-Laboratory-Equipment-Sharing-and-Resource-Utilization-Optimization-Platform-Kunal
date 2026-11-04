
package com.labresource.platform.notification.service;

import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public Notification createNotification(
            Long userId, String title, String message, String type) {

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
                        : type.trim().toUpperCase()
        );
        notification.setIsRead(false);

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You cannot update another user's notification");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        return notificationRepository.save(notification);
    }
}
