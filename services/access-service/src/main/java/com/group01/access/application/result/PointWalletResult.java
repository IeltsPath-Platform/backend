package com.group01.access.application.result;

import java.time.Instant;
import java.util.UUID;

public record PointWalletResult(
        UUID userId,
        long balance,
        long totalCredited,
        long totalDebited,
        Instant updatedAt
        ) {

}
