package com.group01.access.api.dto.response;

import com.group01.access.application.result.GeneratedKeyItem;

import java.time.Instant;
import java.util.UUID;

public record GeneratedKeyResponse(
        UUID id,
        String rawKey,
        String codeHint,
        Instant expiresAt
        ) {

    public static GeneratedKeyResponse from(GeneratedKeyItem item) {
        return new GeneratedKeyResponse(item.id(), item.rawKey(), item.codeHint(), item.expiresAt());
    }
}
