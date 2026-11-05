package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.result.LoginHistoryResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoginHistoryResponse(
        UUID id,
        boolean success,
        String failureReason,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
    public static LoginHistoryResponse from(LoginHistoryResult result) {
        return new LoginHistoryResponse(
                result.id(),
                result.success(),
                result.failureReason(),
                result.ipAddress(),
                result.userAgent(),
                result.createdAt()
        );
    }
}
