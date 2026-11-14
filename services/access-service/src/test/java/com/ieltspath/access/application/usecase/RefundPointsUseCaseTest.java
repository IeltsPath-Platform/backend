package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.RefundPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.repository.PointWalletRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
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

    @Test
    @DisplayName("A repeated refund with the same key returns the stored entry without crediting again")
    void shouldReplayRefundWithSameKey() {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        PointLedgerEntry existing = new PointLedgerEntry(UUID.randomUUID(), userId, 10, 40,
                PointTransactionType.AI_GRADING_REFUND, "GRADING_JOB", refId, "idem-ref", "Refund failed grading job", Instant.now());
        when(pointLedgerRepository.findByIdempotencyKey("idem-ref")).thenReturn(Optional.of(existing));

        PointLedgerResult result = refundPointsUseCase.execute(new RefundPointsCommand(
                userId, 10, "GRADING_JOB", refId, "idem-ref", "Refund failed grading job"));

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.balanceAfter()).isEqualTo(40);
        verifyNoInteractions(pointWalletRepository);
        verify(pointLedgerRepository, never()).save(any());
    }
}
