package com.ieltspath.access.domain.entity;

import com.ieltspath.access.domain.vo.LearningRewardReason;

import java.util.UUID;

/** Stored result for a credit idempotency key, including capped requests that created no ledger row. */
public record PointCreditRequest(
        String idempotencyKey,
        UUID userId,
        int amount,
        LearningRewardReason reason,
        String referenceId,
        int granted,
        boolean capped,
        long balance,
        UUID transactionId
) {}
