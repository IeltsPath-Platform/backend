package com.ieltspath.notification.domain.vo;

import java.time.Instant;
import java.util.UUID;

/** Device registration data; the FCM token is deliberately omitted from toString(). */
public record NotificationDevice(UUID id, UUID userId, UUID installationId, String provider, String platform,
                                 String token, String tokenHash, String appVersion, boolean active,
                                 long registrationVersion, Instant updatedAt) {
    @Override
    public String toString() {
        return "NotificationDevice[id=" + id + ", userId=" + userId + ", installationId=" + installationId
                + ", provider=" + provider + ", platform=" + platform + ", token=[REDACTED], active=" + active
                + ", registrationVersion=" + registrationVersion + "]";
    }
}
