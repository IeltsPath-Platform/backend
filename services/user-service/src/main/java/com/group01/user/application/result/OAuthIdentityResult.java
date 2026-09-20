package com.group01.user.application.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record OAuthIdentityResult(
        UUID id,
        UUID userId,
        String provider,
        String providerSubject,
        LocalDateTime linkedAt,
        LocalDateTime lastAuthenticatedAt
) {
}

