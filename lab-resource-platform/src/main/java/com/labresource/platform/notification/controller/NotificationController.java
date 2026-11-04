
package com.labresource.platform.notification.controller;

import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public List<Notification> getUserNotifications(
            @PathVariable Long userId) {
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public List<Notification> getUnreadNotifications(
            @PathVariable Long userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        return Map.of(
                "unreadCount",
                notificationService.getUnreadCount(userId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_MANAGER', 'SYSTEM_ADMIN')")
    public Notification createNotification(
            @RequestBody Map<String, String> body) {

        Long userId = Long.valueOf(body.get("userId"));

        return notificationService.createNotification(
                userId,
                body.get("title"),
                body.get("message"),
                body.get("type")
        );
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'LAB_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long notificationId,
            @RequestBody Map<String, String> body) {

        Long userId = Long.valueOf(body.get("userId"));

        Notification notification = notificationService.markAsRead(
                notificationId, userId);

        return ResponseEntity.ok(notification);
    }
}
