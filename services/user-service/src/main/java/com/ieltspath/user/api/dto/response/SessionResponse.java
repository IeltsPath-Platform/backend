package com.ieltspath.user.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(UUID sessionId, LocalDateTime startedAt, LocalDateTime lastUsedAt,
                              String ipAddress, String userAgent, Boolean current) {
}
