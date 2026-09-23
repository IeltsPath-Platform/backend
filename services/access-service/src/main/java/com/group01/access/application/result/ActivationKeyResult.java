package com.group01.access.application.result;

import com.group01.access.domain.vo.KeyStatus;

import java.time.Instant;
import java.util.UUID;

public record ActivationKeyResult(
        UUID id,
        UUID productId,
        String codeHint,
        KeyStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant redeemedAt
        ) {

}
