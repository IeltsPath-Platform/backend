package com.group01.access.api.dto.response;

import com.group01.access.application.result.SubscriptionResult;
import com.group01.access.domain.vo.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
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

    public static SubscriptionResponse from(SubscriptionResult r) {
        return new SubscriptionResponse(
                r.id(),
                r.userId(),
                r.planId(),
                r.planCode(),
                r.planName(),
                r.status(),
                r.startsAt(),
                r.endsAt(),
                r.humanGradingCreditsTotal(),
                r.humanGradingCreditsUsed(),
                r.remainingCredits(),
                r.createdAt(),
                r.updatedAt()
        );
    }
}
