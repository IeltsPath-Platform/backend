package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.exception.InsufficientPointsException;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
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
    @DisplayName("Should record course unlock debits with their dedicated ledger type")
    void shouldRecordCourseUnlockDebit() {
        UUID userId = UUID.randomUUID();
        PointWallet wallet = new PointWallet(userId, 50, 50, 0, 0, Instant.now());
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(pointLedgerRepository.save(any(PointLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        PointLedgerResult result = pointDebitWriter.debit(new DebitPointsCommand(
                userId, userId, 8, "COURSE_UNLOCK", UUID.randomUUID(), "idem-course", "Course unlock"));

        assertThat(result.transactionType()).isEqualTo(PointTransactionType.COURSE_UNLOCK_DEBIT);
        assertThat(result.delta()).isEqualTo(-8);
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

    @Test
    @DisplayName("Heart refills and streak freezes keep their own ledger types")
    void shouldRecordHeartRefillAndStreakFreezeTypes() {
        UUID userId = UUID.randomUUID();
        PointWallet wallet = new PointWallet(userId, 50, 50, 0, 0, Instant.now());
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(pointLedgerRepository.save(any(PointLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        PointLedgerResult heart = pointDebitWriter.debit(new DebitPointsCommand(
                userId, userId, 5, "HEART_REFILL", UUID.randomUUID(), "idem-heart", "Heart refill"));
        PointLedgerResult freeze = pointDebitWriter.debit(new DebitPointsCommand(
                userId, userId, 10, "STREAK_FREEZE", UUID.randomUUID(), "idem-freeze", "Streak freeze"));

        assertThat(heart.transactionType()).isEqualTo(PointTransactionType.HEART_REFILL_DEBIT);
        assertThat(freeze.transactionType()).isEqualTo(PointTransactionType.STREAK_FREEZE_DEBIT);
        assertThat(freeze.balanceAfter()).isEqualTo(35);
    }

    @Test
    @DisplayName("A debit equal to the balance leaves the wallet at zero")
    void shouldDebitWholeBalance() {
        UUID userId = UUID.randomUUID();
        PointWallet wallet = new PointWallet(userId, 10, 10, 0, 0, Instant.now());
        when(pointWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(pointLedgerRepository.save(any(PointLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        PointLedgerResult result = pointDebitWriter.debit(new DebitPointsCommand(
                userId, userId, 10, "GRADING_JOB", UUID.randomUUID(), "idem-all", "AI grading"));

        assertThat(result.delta()).isEqualTo(-10);
        assertThat(result.balanceAfter()).isZero();
    }
}
