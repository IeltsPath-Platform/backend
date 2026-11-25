package com.ieltspath.user.domain.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private UUID familyId;
    private LocalDateTime sessionStartedAt;
    private String revokedReason;
    private UUID replacedById;
    private LocalDateTime lastUsedAt;
    private String ipAddress;
    private String userAgent;

    public boolean isUsable(LocalDateTime now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime revokedAt) {
        revoke(revokedAt, null);
    }

    public void revoke(LocalDateTime revokedAt, String reason) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
            this.revokedReason = reason;
        }
    }
}
