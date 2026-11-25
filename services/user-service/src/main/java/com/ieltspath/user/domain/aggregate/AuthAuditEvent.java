package com.ieltspath.user.domain.aggregate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuthAuditEvent(UUID id, UUID userId, String eventType, String ipAddress, String userAgent,
                             String correlationId, Map<String, Object> detail, LocalDateTime createdAt) {
    public AuthAuditEvent {
        if (eventType == null || eventType.isBlank() || eventType.length() > 40) {
            throw new IllegalArgumentException("Loại sự kiện bảo mật không hợp lệ");
        }
        correlationId = correlationId == null || correlationId.length() <= 64
                ? correlationId
                : correlationId.substring(0, 64);
        detail = detail == null ? Map.of() : Map.copyOf(detail);
    }
}
