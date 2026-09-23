package com.group01.access.api.dto.response;

import com.group01.access.application.result.UserEntitlementResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserEntitlementResponse(
        UUID userId,
        boolean isPremium,
        Instant premiumEndsAt,
        int remainingHumanGradingCredits,
        long pointBalance,
        List<String> enabledFeatures
        ) {

    public static UserEntitlementResponse from(UserEntitlementResult r) {
        return new UserEntitlementResponse(
                r.userId(),
                r.isPremium(),
                r.premiumEndsAt(),
                r.remainingHumanGradingCredits(),
                r.pointBalance(),
                r.enabledFeatures()
        );
    }
}
