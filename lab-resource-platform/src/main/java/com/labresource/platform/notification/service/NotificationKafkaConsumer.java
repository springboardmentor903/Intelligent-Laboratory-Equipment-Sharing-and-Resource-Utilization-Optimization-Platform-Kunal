
package com.labresource.platform.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labresource.platform.notification.dto.NotificationEventMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationDeliveryService deliveryService;

    public NotificationKafkaConsumer(
            ObjectMapper objectMapper,
            NotificationDeliveryService deliveryService
    ) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = "lab-events",
            groupId = "lab-resource-group"
    )
    public void consumeNotification(String payload) {

        if (payload == null || payload.isBlank()) {
            log.warn("Skipping empty Kafka message");
            return;
        }

        try {
            NotificationEventMessage event =
                    objectMapper.readValue(
                            payload,
                            NotificationEventMessage.class
                    );

            if (event.getNotificationId() == null
                    || event.getUserId() == null) {
                log.warn("Skipping invalid notification event: {}", payload);
                return;
            }

            log.info(
                    "Processing notification event: notificationId={}, userId={}",
                    event.getNotificationId(),
                    event.getUserId()
            );

            deliveryService.processNotification(
                    event.getNotificationId()
            );

            log.info(
                    "Notification {} processed successfully",
                    event.getNotificationId()
            );

        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {

            log.error(
                    "Skipping non-JSON Kafka message: {}",
                    payload
            );

            // Do not throw: acknowledge malformed test messages
            // so they do not block the Kafka consumer.

        } catch (Exception ex) {

            log.error(
                    "Notification processing failed. Payload={}",
                    payload,
                    ex
            );

            throw new IllegalStateException(
                    "Notification processing failed",
                    ex
            );
        }
    }
}