package com.ieltspath.access.infrastructure.scheduler;

import com.ieltspath.access.application.usecase.ReconcileWalletsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "access.points.reconciliation.enabled", havingValue = "true", matchIfMissing = false)
public class WalletReconciliationScheduler {
    private final ReconcileWalletsUseCase useCase;

    public WalletReconciliationScheduler(ReconcileWalletsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${access.points.reconciliation.interval:PT24H}")
    public void reconcile() {
        useCase.execute();
    }
}
