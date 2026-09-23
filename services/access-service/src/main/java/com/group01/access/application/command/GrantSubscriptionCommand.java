package com.group01.access.application.command;

import java.util.UUID;

public record GrantSubscriptionCommand(
        UUID userId,
        UUID planId,
        int durationDays,
        int humanGradingCredits
        ) {

}
