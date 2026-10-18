package com.ieltspath.events.points;

import java.util.UUID;

/** Learning asks Access to credit points; the grant id is also the AMQP message id and the idempotency key. */
public record PointsGrantRequested(UUID grantId, UUID userId, int amount, String reason, String referenceId) { }
