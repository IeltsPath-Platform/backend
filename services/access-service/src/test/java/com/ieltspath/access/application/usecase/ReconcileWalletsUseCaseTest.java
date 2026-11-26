package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.port.WalletReconciliationStore;
import com.ieltspath.access.application.result.WalletMismatch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconcileWalletsUseCaseTest {
    private final WalletReconciliationStore store = mock(WalletReconciliationStore.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @Test
    void consistentWalletsProduceNoMismatchAndNoMetric() {
        when(store.findMismatches(100)).thenReturn(List.of());

        int count = useCase().execute();

        assertThat(count).isZero();
        assertThat(meters.get("points.reconciliation.mismatch").counter().count()).isZero();
        verify(store).findMismatches(100);
    }

    @Test
    void reportsEachMismatchAndIncrementsCounterWithoutMutatingWallets() {
        WalletMismatch mismatch = new WalletMismatch(UUID.randomUUID(), 15, 10, 10, 10, 0);
        when(store.findMismatches(100)).thenReturn(List.of(mismatch));

        int count = useCase().execute();

        assertThat(count).isEqualTo(1);
        assertThat(meters.get("points.reconciliation.mismatch").counter().count()).isEqualTo(1);
        assertThat(mismatch.balance()).isEqualTo(15);
        verify(store).findMismatches(100);
    }

    private ReconcileWalletsUseCase useCase() {
        return new ReconcileWalletsUseCase(store, meters, 100);
    }
}
