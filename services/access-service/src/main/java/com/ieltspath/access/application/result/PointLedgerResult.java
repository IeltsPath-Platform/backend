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
        UUID referenceId,
        String description,
        Instant createdAt
        ) {

}
