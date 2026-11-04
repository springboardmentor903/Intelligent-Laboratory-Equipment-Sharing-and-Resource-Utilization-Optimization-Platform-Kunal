package com.labresource.platform.resource.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LiveStatusEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public LiveStatusEventPublisher(
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishResourceUpdate(Long resourceId, String eventType) {

        Map<String, Object> event = new HashMap<>();

        event.put("resourceId", resourceId);
        event.put("eventType", eventType);
        event.put("message", "Equipment live status updated");

        messagingTemplate.convertAndSend(
                "/topic/resources/live-status",
                (Object) event
        );
    }
}