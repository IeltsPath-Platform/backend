package com.group01.user.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActionTokenResponse(
        UUID id,
        UUID userId,
        String purpose,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {
}

