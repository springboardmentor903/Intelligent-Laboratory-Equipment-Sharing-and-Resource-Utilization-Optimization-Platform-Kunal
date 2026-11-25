
package com.labresource.platform.notification.repository;

import com.labresource.platform.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.userId = :userId
          AND (:unread IS NULL OR n.isRead = :unread)
          AND (:type IS NULL OR n.type = :type)
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findFilteredNotifications(
            @Param("userId") Long userId,
            @Param("unread") Boolean unread,
            @Param("type") String type,
            Pageable pageable
    );

    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    long countByUserIdAndIsRead(Long userId, Boolean isRead);

    Optional<Notification> findByIdAndUserId(
            Long id,
            Long userId
    );

    boolean existsByUserIdAndReferenceTypeAndReferenceIdAndTypeAndCreatedAtAfter(
            Long userId,
            String referenceType,
            Long referenceId,
            String type,
            LocalDateTime createdAt
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = :readAt
        WHERE n.userId = :userId
          AND n.isRead = false
        """)
    int markAllAsRead(
            @Param("userId") Long userId,
            @Param("readAt") LocalDateTime readAt
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.createdAt < :cutoff
          AND NOT EXISTS (
              SELECT d.id FROM NotificationDelivery d
              WHERE d.notification = n
          )
        ORDER BY n.createdAt ASC
        """)
    List<Notification> findNotificationsWithoutDelivery(
            @Param("cutoff") LocalDateTime cutoff
    );
    List<Notification> findTop3ByUserIdOrderByCreatedAtDesc(
            Long userId
    );
}
