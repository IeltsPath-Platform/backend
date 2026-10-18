package com.ieltspath.access.api.dto.response;

import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.vo.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointLedgerResponse(
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

    public static PointLedgerResponse from(PointLedgerResult r) {
        return new PointLedgerResponse(
                r.id(),
                r.userId(),
                r.delta(),
                r.balanceAfter(),
                r.transactionType(),
                r.referenceType(),
                r.referenceId(),
                r.description(),
                r.createdAt()
        );
    }

    public PointLedgerResponse(UUID id, UUID userId, long delta, long balanceAfter,
                               PointTransactionType transactionType, String referenceType, UUID referenceId,
                               String description, Instant createdAt) {
        this(id, userId, delta, balanceAfter, transactionType, referenceType,
                referenceId == null ? null : referenceId.toString(), description, createdAt);
    }
}
