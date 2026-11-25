package com.ieltspath.user.application.result;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuthAuditEventResult(UUID id, UUID userId, String eventType, String ipAddress, String userAgent,
                                   String correlationId, Map<String, Object> detail, LocalDateTime createdAt) {
}
