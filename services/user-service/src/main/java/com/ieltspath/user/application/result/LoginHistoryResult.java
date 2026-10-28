package com.ieltspath.user.application.result;

import com.ieltspath.user.domain.aggregate.LoginAttempt;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoginHistoryResult(
        UUID id,
        boolean success,
        String failureReason,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
    public static LoginHistoryResult from(LoginAttempt attempt) {
        return new LoginHistoryResult(
                attempt.getId(),
                attempt.isSuccess(),
                attempt.getFailureReason() == null ? null : attempt.getFailureReason().name(),
                attempt.getIpAddress(),
                attempt.getUserAgent(),
                attempt.getCreatedAt()
        );
    }
}
