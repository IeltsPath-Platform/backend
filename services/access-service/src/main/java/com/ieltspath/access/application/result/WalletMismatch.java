package com.ieltspath.access.application.result;

import java.util.UUID;

public record WalletMismatch(
        UUID userId,
        long balance,
        long ledgerDeltaSum,
        long latestLedgerBalance,
        long totalCredited,
        long totalDebited) {
}
