package com.group01.user.application.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountActionTokenResult(
        UUID id,
        UUID userId,
        String purpose,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {
}

