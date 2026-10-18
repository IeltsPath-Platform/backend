package com.ieltspath.access.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record HumanGradingCreditConsumption(UUID submissionId, UUID userId, UUID subscriptionId,
                                             int remainingCredits, Instant refundedAt) {}
