package com.group01.access.api.dto.response;

import com.group01.access.application.result.ActivationResult;
import com.group01.access.domain.vo.KeyType;

import java.time.Instant;
import java.util.UUID;

public record ActivationResponse(
        UUID activationId,
        UUID keyId,
        UUID userId,
        KeyType productType,
        int pointsGranted,
        int premiumDaysGranted,
        int humanGradingCreditsGranted,
        Instant activatedAt,
        Long newBalance,
        Instant subscriptionEndsAt
        ) {

    public static ActivationResponse from(ActivationResult r) {
        return new ActivationResponse(
                r.activationId(),
                r.keyId(),
                r.userId(),
                r.productType(),
                r.pointsGranted(),
                r.premiumDaysGranted(),
                r.humanGradingCreditsGranted(),
                r.activatedAt(),
                r.newBalance(),
                r.subscriptionEndsAt()
        );
    }
}
