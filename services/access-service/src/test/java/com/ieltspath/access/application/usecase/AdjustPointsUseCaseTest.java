package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.AdjustPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.exception.InsufficientPointsException;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.repository.PointWalletRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdjustPointsUseCaseTest {
    private final PointWalletRepository wallets = mock(PointWalletRepository.class);
    private final PointLedgerRepository ledger = mock(PointLedgerRepository.class);
    private final AdjustPointsUseCase useCase = new AdjustPointsUseCase(wallets, ledger);
    private final UUID userId = UUID.randomUUID();
    private PointWallet wallet;

    @BeforeEach
    void walletWithThirtyPoints() {
        wallet = new PointWallet(userId, 30, 30, 0, 0, Instant.now());
        when(ledger.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(wallets.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(ledger.save(any(PointLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aPositiveAdjustmentCreditsTheWallet() {
        PointLedgerResult result = useCase.execute(new AdjustPointsCommand(userId, 20, "Event bonus", "idem-adj"));

        assertThat(wallet.getBalance()).isEqualTo(50);
        assertThat(result.delta()).isEqualTo(20);
        assertThat(result.balanceAfter()).isEqualTo(50);
        assertThat(result.transactionType()).isEqualTo(PointTransactionType.ADMIN_ADJUSTMENT);
        assertThat(result.referenceType()).isEqualTo("ADMIN");
        verify(wallets).save(wallet);
    }

    @Test
    void aNegativeAdjustmentAboveTheBalanceIsRefused() {
        assertThatThrownBy(() -> useCase.execute(new AdjustPointsCommand(userId, -100, "Correction", "idem-neg")))
                .isInstanceOf(InsufficientPointsException.class);
        assertThat(wallet.getBalance()).isEqualTo(30);
        verify(wallets, never()).save(any());
        verify(ledger, never()).save(any());
    }

    @Test
    void aZeroAdjustmentIsRefused() {
        assertThatThrownBy(() -> useCase.execute(new AdjustPointsCommand(userId, 0, "Nothing", "idem-zero")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit amount must be greater than 0");
        verify(ledger, never()).save(any());
    }
}
