package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.result.AuthAuditEventResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuthAuditEventResponse(UUID id, String eventType, String ipAddress, String userAgent,
                                     String correlationId, Map<String, Object> detail, LocalDateTime createdAt) {
    public static AuthAuditEventResponse from(AuthAuditEventResult result) {
        return new AuthAuditEventResponse(result.id(), result.eventType(), result.ipAddress(), result.userAgent(),
                result.correlationId(), result.detail(), result.createdAt());
    }
}
