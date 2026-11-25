package com.ieltspath.user.application.port;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenRevocationStore {
    void revokeSession(UUID sessionId);
    void revokeUser(UUID userId, Instant revokedAt);
}
