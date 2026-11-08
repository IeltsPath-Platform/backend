package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.NotificationDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterNotificationDeviceUseCase {
    private final NotificationDeviceRepository devices;

    public RegisterNotificationDeviceUseCase(NotificationDeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public void execute(UUID userId, UUID installationId, String provider, String platform, String token, String appVersion) {
        if (userId == null || installationId == null || !"FCM".equals(provider) || !"ANDROID".equals(platform)
                || token == null || token.isBlank() || token.length() > 4096
                || appVersion != null && appVersion.length() > 128) {
            throw new IllegalArgumentException("Invalid device registration");
        }
        devices.register(userId, installationId, provider, platform, token, appVersion);
    }
}
