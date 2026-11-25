
package com.labresource.platform.notification.controller;

import com.labresource.platform.notification.dto.NotificationResponse;
import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.notification.service.NotificationService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<NotificationResponse> getMyNotifications(
            Authentication authentication,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = currentUser(authentication).getId();

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination values"
            );
        }

        return notificationService.getUserNotifications(
                userId,
                unread,
                type,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        ).map(NotificationResponse::new);
    }

    @GetMapping("/latest")
    public List<NotificationResponse> latest(
            Authentication authentication
    ) {
        return notificationService
                .getLatestNotifications(currentUser(authentication).getId())
                .stream()
                .map(NotificationResponse::new)
                .toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(
            Authentication authentication
    ) {
        return Map.of(
                "unreadCount",
                notificationService.getUnreadCount(
                        currentUser(authentication).getId()
                )
        );
    }

    // UI TEST NOTIFICATION API
    @PostMapping("/test")
    public ResponseEntity<NotificationResponse> createTestNotification(
            Authentication authentication,
            @RequestBody TestNotificationRequest request
    ) {
        Long userId = currentUser(authentication).getId();

        if (request == null
                || request.getTitle() == null
                || request.getTitle().isBlank()
                || request.getMessage() == null
                || request.getMessage().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title and message are required"
            );
        }

        String title = request.getTitle().trim();
        String message = request.getMessage().trim();
        String type = request.getType() == null
                || request.getType().isBlank()
                ? "GENERAL"
                : request.getType().trim().toUpperCase();

        if (title.length() > 100
                || message.length() > 2000
                || type.length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Input exceeds allowed length"
            );
        }

        Notification notification =
                notificationService.createNotification(
                        userId,
                        title,
                        message,
                        type
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new NotificationResponse(notification));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        Notification notification = notificationService.markAsRead(
                notificationId,
                currentUser(authentication).getId()
        );

        return new NotificationResponse(notification);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead(
            Authentication authentication
    ) {
        int updated = notificationService.markAllAsRead(
                currentUser(authentication).getId()
        );

        return Map.of("updatedCount", updated);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return userRepository
                .findByEmailWithProfile(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    public static class TestNotificationRequest {

        private String title;
        private String message;
        private String type;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
