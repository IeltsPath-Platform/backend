package com.group01.access.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserEntitlementResult(
        UUID userId,
        boolean isPremium,
        Instant premiumEndsAt,
        int remainingHumanGradingCredits,
        long pointBalance,
        List<String> enabledFeatures
        ) {

}
