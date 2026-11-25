
package com.labresource.platform.notification.repository;

import com.labresource.platform.notification.entity.NotificationDelivery;
import com.labresource.platform.notification.entity.NotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByNotificationIdAndChannel(
            Long notificationId, String channel);

    boolean existsByNotificationIdAndChannel(
            Long notificationId, String channel);

    List<NotificationDelivery> findByStatusAndNextRetryAtLessThanEqual(
            NotificationDeliveryStatus status,
            LocalDateTime currentTime);

    List<NotificationDelivery> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            NotificationDeliveryStatus status,
            LocalDateTime currentTime);

    List<NotificationDelivery> findByNotificationId(Long notificationId);
}
