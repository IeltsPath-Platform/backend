package com.group01.access.application.result;

import com.group01.access.domain.vo.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResult(
        UUID id,
        UUID userId,
        UUID planId,
        String planCode,
        String planName,
        SubscriptionStatus status,
        Instant startsAt,
        Instant endsAt,
        int humanGradingCreditsTotal,
        int humanGradingCreditsUsed,
        int remainingCredits,
        Instant createdAt,
        Instant updatedAt
        ) {

}
