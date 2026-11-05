package com.ieltspath.user.application.port;

import java.util.UUID;

public interface AvatarUploadRateLimiter {
    void checkAndAcquire(UUID userId);
}
