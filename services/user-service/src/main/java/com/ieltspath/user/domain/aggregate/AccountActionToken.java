package com.ieltspath.user.domain.aggregate;

import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AccountActionToken {
    private UUID id;
    private UUID userId;
    private ActionTokenPurpose purpose;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    public void use() {
        if (isUsed()) {
            throw new IllegalStateException("Token này đã được sử dụng");
        }
        if (isExpired()) {
            throw new IllegalStateException("Token này đã hết hạn");
        }
        this.usedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }
}

