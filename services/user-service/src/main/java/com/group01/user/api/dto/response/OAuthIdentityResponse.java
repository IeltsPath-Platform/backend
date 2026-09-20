package com.group01.user.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record OAuthIdentityResponse(
        UUID id,
        UUID userId,
        String provider,
        String providerSubject,
        LocalDateTime linkedAt,
        LocalDateTime lastAuthenticatedAt
) {
}

