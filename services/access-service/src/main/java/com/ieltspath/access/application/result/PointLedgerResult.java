package com.ieltspath.access.application.result;

import com.ieltspath.access.domain.vo.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointLedgerResult(
        UUID id,
        UUID userId,
        long delta,
        long balanceAfter,
        PointTransactionType transactionType,
        String referenceType,
        String referenceId,
        String description,
        Instant createdAt
        ) {

    public PointLedgerResult(UUID id, UUID userId, long delta, long balanceAfter,
                             PointTransactionType transactionType, String referenceType, UUID referenceId,
                             String description, Instant createdAt) {
        this(id, userId, delta, balanceAfter, transactionType, referenceType,
                referenceId == null ? null : referenceId.toString(), description, createdAt);
    }

}
