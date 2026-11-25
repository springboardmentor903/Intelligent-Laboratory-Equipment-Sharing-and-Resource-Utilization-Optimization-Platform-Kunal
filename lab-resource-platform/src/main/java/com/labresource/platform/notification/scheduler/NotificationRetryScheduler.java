
package com.labresource.platform.notification.scheduler;

import com.labresource.platform.notification.entity.NotificationDelivery;
import com.labresource.platform.notification.entity.NotificationDeliveryStatus;
import com.labresource.platform.notification.repository.NotificationDeliveryRepository;
import com.labresource.platform.notification.service.NotificationDeliveryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NotificationRetryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationRetryScheduler.class);

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryService deliveryService;

    public NotificationRetryScheduler(
            NotificationDeliveryRepository deliveryRepository,
            NotificationDeliveryService deliveryService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelay = 300000)
    public void retryFailedDeliveries() {

        List<NotificationDelivery> failedDeliveries =
                deliveryRepository
                        .findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                                NotificationDeliveryStatus.FAILED,
                                LocalDateTime.now()
                        );

        for (NotificationDelivery delivery : failedDeliveries) {
            try {
                int attempts = delivery.getAttemptCount() == null
                        ? 0
                        : delivery.getAttemptCount();

                if (attempts >= MAX_ATTEMPTS) {
                    delivery.setNextRetryAt(null);
                    deliveryRepository.save(delivery);

                    log.warn(
                            "Retry limit reached. Delivery ID={}, attempts={}",
                            delivery.getId(),
                            attempts
                    );
                    continue;
                }

                // Retry only this delivery, not every channel.
                deliveryService.retryDelivery(delivery.getId());

            } catch (Exception ex) {
                log.error(
                        "Retry failed for delivery ID={}",
                        delivery.getId(),
                        ex
                );
            }
        }
    }
}
