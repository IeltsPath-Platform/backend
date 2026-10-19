package com.ieltspath.community.application.port;

import java.util.UUID;

public interface MediaRateLimiter {

    void checkAndAcquire(UUID userId);
}

