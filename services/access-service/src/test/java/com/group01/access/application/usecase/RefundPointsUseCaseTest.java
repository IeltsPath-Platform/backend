package com.group01.access.application.usecase;

import com.group01.access.application.command.RefundPointsCommand;
import com.group01.access.application.result.PointLedgerResult;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.entity.PointLedgerEntry;
import com.group01.access.domain.repository.PointLedgerRepository;
import com.group01.access.domain.repository.PointWalletRepository;
import com.group01.access.domain.vo.PointTransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundPointsUseCaseTest {

    @Mock
    private PointWalletRepository pointWalletRepository;
    @Mock
    private PointLedgerRepository pointLedgerRepository;

    @InjectMocks
    private RefundPointsUseCase refundPointsUseCase;

    @Test
    @DisplayName("Should successfully refund points to user wallet")
    void shouldRefundPointsSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        PointWallet wallet = new PointWallet(userId, 30, 50, 20, 0, Instant.now());

        when(pointLedgerRepository.findByIdempotencyKey("idem-ref")).thenReturn(Optional.empty());
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(pointLedgerRepository.save(any(PointLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        PointLedgerResult result = refundPointsUseCase.execute(new RefundPointsCommand(
                userId, 10, "GRADING_JOB", refId, "idem-ref", "Refund failed grading job"
        ));

        assertThat(result).isNotNull();
        assertThat(result.delta()).isEqualTo(10);
        assertThat(result.balanceAfter()).isEqualTo(40);
        assertThat(result.transactionType()).isEqualTo(PointTransactionType.AI_GRADING_REFUND);

        verify(pointWalletRepository).save(wallet);
        verify(pointLedgerRepository).save(any(PointLedgerEntry.class));
    }
}
