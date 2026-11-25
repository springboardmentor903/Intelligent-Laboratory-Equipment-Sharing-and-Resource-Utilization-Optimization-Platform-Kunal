
package com.labresource.platform.notification.controller;

import com.labresource.platform.notification.dto.NotificationPreferenceRequest;
import com.labresource.platform.notification.dto.NotificationPreferenceResponse;
import com.labresource.platform.notification.dto.PushDeviceTokenRequest;
import com.labresource.platform.notification.entity.NotificationPreference;
import com.labresource.platform.notification.entity.PushDeviceToken;
import com.labresource.platform.notification.service.NotificationPreferenceService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    public NotificationPreferenceController(
            NotificationPreferenceService preferenceService,
            UserRepository userRepository
    ) {
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/preferences")
    public NotificationPreferenceResponse getPreferences(
            Authentication authentication
    ) {
        User user = currentUser(authentication);

        NotificationPreference preference =
                preferenceService.getOrCreatePreference(user.getId());

        boolean hasPhone = user.getPhone() != null
                && !user.getPhone().isBlank();

        return new NotificationPreferenceResponse(
                preference,
                hasPhone
        );
    }

    @PutMapping("/preferences")
    public NotificationPreferenceResponse updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication
    ) {
        User user = currentUser(authentication);

        try {
            NotificationPreference preference =
                    preferenceService.updatePreference(
                            user.getId(),
                            request
                    );

            boolean hasPhone = user.getPhone() != null
                    && !user.getPhone().isBlank();

            return new NotificationPreferenceResponse(
                    preference,
                    hasPhone
            );

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );
        }
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> getDevices(
            Authentication authentication
    ) {
        Long userId = currentUser(authentication).getId();

        return preferenceService.getMyDevices(userId)
                .stream()
                .map(device -> Map.<String, Object>of(
                        "id", device.getId(),
                        "platform", device.getPlatform(),
                        "deviceName", device.getDeviceName() == null
                                ? ""
                                : device.getDeviceName(),
                        "active", device.getActive(),
                        "lastUsedAt", device.getLastUsedAt()
                ))
                .toList();
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registerDevice(
            @Valid @RequestBody PushDeviceTokenRequest request,
            Authentication authentication
    ) {
        Long userId = currentUser(authentication).getId();

        try {
            PushDeviceToken device =
                    preferenceService.registerDevice(
                            userId,
                            request.getDeviceToken(),
                            request.getPlatform(),
                            request.getDeviceName()
                    );

            return Map.of(
                    "message", "Device registered successfully",
                    "deviceId", device.getId(),
                    "platform", device.getPlatform()
            );

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );
        }
    }

    @DeleteMapping("/devices")
    public Map<String, String> removeDevice(
            @RequestParam String token,
            Authentication authentication
    ) {
        Long userId = currentUser(authentication).getId();

        try {
            preferenceService.removeDevice(userId, token);

            return Map.of(
                    "message",
                    "Device removed successfully"
            );

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage()
            );
        }
    }

    @DeleteMapping("/devices/all")
    public Map<String, String> removeAllDevices(
            Authentication authentication
    ) {
        Long userId = currentUser(authentication).getId();

        preferenceService.removeAllDevices(userId);

        return Map.of(
                "message",
                "All devices removed successfully"
        );
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
}
