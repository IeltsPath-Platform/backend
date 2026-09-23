package com.group01.access.application.command;

import java.util.UUID;

public record DebitPointsCommand(
        UUID userId,
        long amount,
        String referenceType,
        UUID referenceId,
        String idempotencyKey,
        String description
        ) {

}
