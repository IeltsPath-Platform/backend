package com.ieltspath.access.application.command;

import com.ieltspath.access.domain.vo.LearningRewardReason;

import java.util.UUID;

public record CreditPointsCommand(
        UUID actorUserId,
        UUID userId,
        int amount,
        LearningRewardReason reason,
        String referenceId,
        String idempotencyKey
) {}
