package com.ieltspath.user.application.port;

import java.util.Optional;
import java.util.UUID;

public interface ReauthTokenStore {
    String issue(UUID userId, String method, UUID sessionId);
    Optional<Grant> consume(String token);

    record Grant(UUID userId, String method, UUID sessionId) { }
}
