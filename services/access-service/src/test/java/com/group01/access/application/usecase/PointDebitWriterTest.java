package com.group01.access.application.usecase;

import com.group01.access.application.command.DebitPointsCommand;
import com.group01.access.application.result.PointLedgerResult;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.entity.PointLedgerEntry;
import com.group01.access.domain.exception.InsufficientPointsException;
import com.group01.access.domain.repository.OutboxEventRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointDebitWriterTest {

    @Mock
    private PointWalletRepository pointWalletRepository;
    @Mock
    private PointLedgerRepository pointLedgerRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private PointDebitWriter pointDebitWriter;

    @Test
    @DisplayName("Should successfully debit points when balance is sufficient")
    void shouldDebitPointsSuccessfully() {
        UUID userId = UUID.randomUUID();
        PointWallet wallet = new PointWallet(userId, 50, 50, 0, 0, Instant.now());
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(pointLedgerRepository.save(any(PointLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        PointLedgerResult result = pointDebitWriter.debit(new DebitPointsCommand(
                userId, userId, 10, "GRADING_JOB", UUID.randomUUID(), "idem-debit", "AI grading"));

        assertThat(result.delta()).isEqualTo(-10);
        assertThat(result.balanceAfter()).isEqualTo(40);
        assertThat(result.transactionType()).isEqualTo(PointTransactionType.AI_GRADING_DEBIT);
        verify(pointWalletRepository).save(wallet);
        verify(outboxEventRepository).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientPointsException when balance is less than required")
    void shouldThrowWhenInsufficientBalance() {
        UUID userId = UUID.randomUUID();
        when(pointWalletRepository.findByUserId(userId))
                .thenReturn(Optional.of(new PointWallet(userId, 5, 5, 0, 0, Instant.now())));

        assertThatThrownBy(() -> pointDebitWriter.debit(new DebitPointsCommand(
                userId, userId, 10, "GRADING_JOB", UUID.randomUUID(), "idem-insuf", "AI grading")))
                .isInstanceOf(InsufficientPointsException.class);
        verifyNoInteractions(pointLedgerRepository);
    }
}
