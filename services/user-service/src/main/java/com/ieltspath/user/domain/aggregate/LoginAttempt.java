package com.ieltspath.user.domain.aggregate;

import com.ieltspath.user.domain.vo.LoginFailureReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoginAttempt {
    private UUID id;
    private UUID userId;
    private boolean success;
    private LoginFailureReason failureReason;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public static LoginAttempt succeeded(UUID userId, String ipAddress, String userAgent) {
        return new LoginAttempt(null, requireUser(userId), true, null, ipAddress, userAgent, null);
    }

    public static LoginAttempt failed(UUID userId, LoginFailureReason reason, String ipAddress, String userAgent) {
        if (reason == null) {
            throw new IllegalArgumentException("Lý do đăng nhập thất bại không được để trống");
        }
        return new LoginAttempt(null, requireUser(userId), false, reason, ipAddress, userAgent, null);
    }

    private static UUID requireUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Người dùng không được để trống");
        }
        return userId;
    }
}
