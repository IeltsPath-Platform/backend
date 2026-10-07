package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.exception.PointsActorMismatchException;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitPointsUseCaseTest {

    @Mock
    private PointLedgerRepository pointLedgerRepository;
    @Mock
    private PointDebitWriter pointDebitWriter;

    @InjectMocks
    private DebitPointsUseCase debitPointsUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID refId = UUID.randomUUID();

    private DebitPointsCommand command(UUID actor, long amount, UUID reference) {
        return new DebitPointsCommand(actor, userId, amount, "LESSON_WRITING", reference, "idem-1", "Writing grading");
    }

    private PointLedgerEntry entry(long delta) {
        return new PointLedgerEntry(UUID.randomUUID(), userId, delta, 17, PointTransactionType.AI_GRADING_DEBIT,
                "LESSON_WRITING", refId, "idem-1", "Writing grading", Instant.now());
    }

    @Test
    @DisplayName("Debit for another user is rejected before touching the wallet")
    void rejectsDebitForAnotherUser() {
        assertThatThrownBy(() -> debitPointsUseCase.execute(command(UUID.randomUUID(), 3, refId)))
                .isInstanceOf(PointsActorMismatchException.class);
        verifyNoInteractions(pointLedgerRepository, pointDebitWriter);
    }

    @Test
    @DisplayName("New key is written by the writer")
    void writesNewDebit() {
        PointLedgerResult written = DebitPointsUseCase.toResult(entry(-3));
        when(pointLedgerRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(pointDebitWriter.debit(any())).thenReturn(written);

        assertThat(debitPointsUseCase.execute(command(userId, 3, refId))).isEqualTo(written);
    }

    @Test
    @DisplayName("Replaying the same debit returns the existing entry without writing")
    void replaysSameDebit() {
        PointLedgerEntry existing = entry(-3);
        when(pointLedgerRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        assertThat(debitPointsUseCase.execute(command(userId, 3, refId)).id()).isEqualTo(existing.getId());
        verifyNoInteractions(pointDebitWriter);
    }

    @Test
    @DisplayName("Reusing a key for a different amount or reference is a conflict")
    void rejectsKeyReuseWithDifferentContent() {
        when(pointLedgerRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(entry(-3)));

        assertThatThrownBy(() -> debitPointsUseCase.execute(command(userId, 5, refId)))
                .isInstanceOf(DuplicateIdempotencyException.class);
        assertThatThrownBy(() -> debitPointsUseCase.execute(command(userId, 3, UUID.randomUUID())))
                .isInstanceOf(DuplicateIdempotencyException.class);
        verifyNoInteractions(pointDebitWriter);
    }

    @Test
    @DisplayName("A concurrent duplicate that loses the unique race replays the winning entry")
    void concurrentDuplicateReplaysWinner() {
        PointLedgerEntry winner = entry(-3);
        when(pointLedgerRepository.findByIdempotencyKey("idem-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(pointDebitWriter.debit(any())).thenThrow(new DataIntegrityViolationException("uq idempotency_key"));

        assertThat(debitPointsUseCase.execute(command(userId, 3, refId)).id()).isEqualTo(winner.getId());
    }

    @Test
    @DisplayName("An integrity error without a committed entry is not hidden")
    void integrityErrorWithoutEntryIsRethrown() {
        when(pointLedgerRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(pointDebitWriter.debit(any())).thenThrow(new DataIntegrityViolationException("other"));

        assertThatThrownBy(() -> debitPointsUseCase.execute(command(userId, 3, refId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
