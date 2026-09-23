package com.group01.access.api.dto.response;

import com.group01.access.application.result.PointLedgerResult;
import com.group01.access.domain.vo.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointLedgerResponse(
        UUID id,
        UUID userId,
        long delta,
        long balanceAfter,
        PointTransactionType transactionType,
        String referenceType,
        UUID referenceId,
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
}
