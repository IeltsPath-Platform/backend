package com.ieltspath.access.application.command;

import java.util.UUID;

public record AdjustPointsCommand(
        UUID userId,
        long delta,
        String reason,
        String idempotencyKey
        ) {

}
