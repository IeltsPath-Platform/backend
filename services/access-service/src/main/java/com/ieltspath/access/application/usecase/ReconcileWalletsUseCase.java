package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.port.WalletReconciliationStore;
import com.ieltspath.access.application.result.WalletMismatch;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ReconcileWalletsUseCase {
    private final WalletReconciliationStore store;
    private final Counter mismatchCounter;
    private final int maxRows;

    public ReconcileWalletsUseCase(WalletReconciliationStore store, MeterRegistry meterRegistry,
                                  @Value("${access.points.reconciliation.max-rows:100}") int maxRows) {
        if (maxRows < 1) throw new IllegalArgumentException("Wallet reconciliation row limit must be positive");
        this.store = store;
        this.mismatchCounter = Counter.builder("points.reconciliation.mismatch")
                .description("Number of wallet and ledger balance mismatches found")
                .register(meterRegistry);
        this.maxRows = maxRows;
    }

    public int execute() {
        List<WalletMismatch> mismatches = store.findMismatches(maxRows);
        for (WalletMismatch mismatch : mismatches) {
            log.warn("Point wallet and ledger mismatch userId={} balance={} ledgerDeltaSum={} latestLedgerBalance={} totalCredited={} totalDebited={}",
                    mismatch.userId(), mismatch.balance(), mismatch.ledgerDeltaSum(), mismatch.latestLedgerBalance(),
                    mismatch.totalCredited(), mismatch.totalDebited());
            mismatchCounter.increment();
        }
        return mismatches.size();
    }
}
