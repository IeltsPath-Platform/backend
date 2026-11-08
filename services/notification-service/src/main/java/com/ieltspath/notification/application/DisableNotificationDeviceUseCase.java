package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.repository.NotificationDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DisableNotificationDeviceUseCase {
    private final NotificationDeviceRepository devices;

    public DisableNotificationDeviceUseCase(NotificationDeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public void execute(UUID userId, UUID installationId) {
        if (userId != null && installationId != null) devices.disable(userId, installationId);
    }
}
