package com.ieltspath.access.api.dto.response;

import com.ieltspath.access.application.result.PointWalletResult;

import java.time.Instant;
import java.util.UUID;

public record PointWalletResponse(
        UUID userId,
        long balance,
        long totalCredited,
        long totalDebited,
        Instant updatedAt
        ) {

    public static PointWalletResponse from(PointWalletResult r) {
        return new PointWalletResponse(r.userId(), r.balance(), r.totalCredited(), r.totalDebited(), r.updatedAt());
    }
}
