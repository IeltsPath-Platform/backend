package com.group01.access.application.result;

import java.time.Instant;
import java.util.UUID;

public record GeneratedKeyItem(
        UUID id,
        String rawKey,
        String codeHint,
        Instant expiresAt
        ) {

}
