
package com.labresource.platform.notification.scheduler;

import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.notification.repository.NotificationRepository;
import com.labresource.platform.notification.service.NotificationKafkaPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotificationPickupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationPickupScheduler.class);

    private final NotificationRepository notificationRepository;
    private final NotificationKafkaPublisher kafkaPublisher;

    public NotificationPickupScheduler(
            NotificationRepository notificationRepository,
            NotificationKafkaPublisher kafkaPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Scheduled(fixedDelay = 120000)
    public void republishNotificationsWithoutDelivery() {

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);

        List<Notification> pendingNotifications =
                notificationRepository
                        .findNotificationsWithoutDelivery(cutoff);

        for (Notification notification : pendingNotifications) {
            try {
                kafkaPublisher.publish(
                        notification.getId(),
                        notification.getUserId()
                );

                log.info(
                        "Pickup republished notification {}",
                        notification.getId()
                );

            } catch (Exception ex) {
                log.error(
                        "Pickup failed for notification {}",
                        notification.getId(),
                        ex
                );
            }
        }
    }
}
