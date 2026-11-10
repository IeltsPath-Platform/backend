package com.ieltspath.notification.domain.repository;

import com.ieltspath.notification.domain.vo.NotificationDevice;

import java.util.List;
import java.util.UUID;

public interface NotificationDeviceRepository {
    void register(UUID userId, UUID installationId, String provider, String platform, String token, String appVersion);
    void disable(UUID userId, UUID installationId);
    List<NotificationDevice> findActiveByUser(UUID userId);
}
