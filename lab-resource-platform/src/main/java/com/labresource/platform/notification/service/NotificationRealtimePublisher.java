package com.labresource.platform.notification.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationRealtimePublisher(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishUnreadCount(Long userId, long unreadCount) {

        if (userId == null || userId <= 0) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", "UNREAD_COUNT_UPDATED",
                "userId", userId,
                "unreadCount", unreadCount
        );

        // Cast to Object to resolve convertAndSend overload ambiguity.
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                (Object) payload
        );
    }
}