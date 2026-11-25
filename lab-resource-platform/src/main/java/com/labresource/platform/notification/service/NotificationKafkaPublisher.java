
package com.labresource.platform.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labresource.platform.notification.dto.NotificationEventMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationKafkaPublisher.class);

    private static final String TOPIC = "lab-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public NotificationKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Long notificationId, Long userId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new NotificationEventMessage(notificationId, userId)
            );

            kafkaTemplate.send(
                    TOPIC,
                    String.valueOf(notificationId),
                    payload
            ).whenComplete((result, error) -> {
                if (error != null) {
                    log.error(
                            "Kafka publish failed for notification {}",
                            notificationId,
                            error
                    );
                } else {
                    log.info(
                            "Notification {} published to Kafka",
                            notificationId
                    );
                }
            });

        } catch (JsonProcessingException ex) {
            log.error(
                    "Unable to serialize notification {}",
                    notificationId,
                    ex
            );
        }
    }
}
