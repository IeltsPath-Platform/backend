package com.group01.access.application.command;

import java.util.UUID;

public record RefundPointsCommand(
        UUID userId,
        long amount,
        String referenceType,
        UUID referenceId,
        String idempotencyKey,
        String description
        ) {

}
