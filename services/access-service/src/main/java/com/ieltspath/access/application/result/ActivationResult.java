package com.ieltspath.access.application.result;

import com.ieltspath.access.domain.vo.KeyType;

import java.time.Instant;
import java.util.UUID;

public record ActivationResult(
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

}
