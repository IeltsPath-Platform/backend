package com.ieltspath.access.application.command;

import java.util.UUID;

public record GrantSubscriptionCommand(
        UUID userId,
        UUID planId,
        int durationDays,
        int humanGradingCredits,
        String idempotencyKey
        ) {

    public GrantSubscriptionCommand(UUID userId, UUID planId, int durationDays, int humanGradingCredits) {
        this(userId, planId, durationDays, humanGradingCredits, java.util.UUID.randomUUID().toString());
    }

}
