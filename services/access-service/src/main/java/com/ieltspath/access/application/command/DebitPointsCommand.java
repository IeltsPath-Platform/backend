package com.ieltspath.access.application.command;

import java.util.UUID;

/**
 * @param actorUserId subject of the calling token; a service may only debit the wallet of the learner it acts for
 */
public record DebitPointsCommand(
        UUID actorUserId,
        UUID userId,
        long amount,
        String referenceType,
        UUID referenceId,
        String idempotencyKey,
        String description
        ) {

}
