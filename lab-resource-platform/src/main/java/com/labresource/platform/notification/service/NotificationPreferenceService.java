
package com.labresource.platform.notification.service;

import com.labresource.platform.notification.dto.NotificationPreferenceRequest;
import com.labresource.platform.notification.entity.NotificationPreference;
import com.labresource.platform.notification.entity.PushDeviceToken;
import com.labresource.platform.notification.repository.NotificationPreferenceRepository;
import com.labresource.platform.notification.repository.PushDeviceTokenRepository;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final PushDeviceTokenRepository deviceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            PushDeviceTokenRepository deviceRepository,
            UserRepository userRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationPreference getOrCreatePreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference preference =
                            new NotificationPreference();
                    preference.setUserId(userId);
                    preference.setEmailEnabled(true);
                    preference.setSmsEnabled(false);
                    preference.setPushEnabled(false);
                    return preferenceRepository.save(preference);
                });
    }

    @Transactional
    public NotificationPreference updatePreference(
            Long userId,
            NotificationPreferenceRequest request
    ) {
        NotificationPreference preference =
                getOrCreatePreference(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }

        if (request.getSmsEnabled() != null) {
            if (request.getSmsEnabled()
                    && (user.getPhone() == null
                    || user.getPhone().isBlank())) {
                throw new IllegalArgumentException(
                        "Add a phone number to your profile before enabling SMS notifications"
                );
            }

            preference.setSmsEnabled(request.getSmsEnabled());
        }

        if (request.getPushEnabled() != null) {
            if (request.getPushEnabled()
                    && !deviceRepository.existsByUserIdAndActiveTrue(userId)) {
                throw new IllegalArgumentException(
                        "Register a push device before enabling push notifications"
                );
            }

            preference.setPushEnabled(request.getPushEnabled());
        }

        return preferenceRepository.save(preference);
    }

    @Transactional
    public PushDeviceToken registerDevice(
            Long userId,
            String token,
            String platform,
            String deviceName
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Device token is required");
        }

        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Platform is required");
        }

        LocalDateTime now = LocalDateTime.now();

        PushDeviceToken device = deviceRepository
                .findByDeviceToken(token)
                .orElseGet(PushDeviceToken::new);

        device.setUserId(userId);
        device.setDeviceToken(token.trim());
        device.setPlatform(platform.trim().toUpperCase());
        device.setDeviceName(deviceName);
        device.setActive(true);
        device.setLastUsedAt(now);

        return deviceRepository.save(device);
    }

    @Transactional
    public void removeDevice(Long userId, String token) {
        PushDeviceToken device = deviceRepository
                .findByDeviceTokenAndUserId(token, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Device token not found for this user"
                        ));

        device.setActive(false);
        deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public List<PushDeviceToken> getMyDevices(Long userId) {
        return deviceRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional
    public void removeAllDevices(Long userId) {
        List<PushDeviceToken> devices =
                deviceRepository.findByUserIdAndActiveTrue(userId);

        for (PushDeviceToken device : devices) {
            device.setActive(false);
        }

        deviceRepository.saveAll(devices);
    }
}
