package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelPlacementAttemptUseCaseTest {
    private final AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
    private final CancelPlacementAttemptUseCase useCase = new CancelPlacementAttemptUseCase(attempts);
    private final UUID owner = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    @Test
    void ownerCanCancelTheirInProgressPlacementAttempt() {
        AssessmentAttempt attempt = attempt(AttemptType.PLACEMENT, AttemptStatus.IN_PROGRESS);
        when(attempts.findByIdAndUserId(attemptId, owner)).thenReturn(Optional.of(attempt));
        when(attempts.save(attempt)).thenReturn(attempt);
        assertThat(useCase.execute(owner, attemptId).status()).isEqualTo(AttemptStatus.CANCELLED);
        verify(attempts).save(attempt);
    }

    @Test
    void anotherOwnerCannotCancelTheAttempt() {
        when(attempts.findByIdAndUserId(attemptId, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(owner, attemptId)).isInstanceOf(AssessmentNotFoundException.class);
        verify(attempts, never()).save(any());
    }

    @Test
    void otherAttemptTypesAndNonOpenPlacementsCannotBeCancelled() {
        for (AssessmentAttempt attempt : new AssessmentAttempt[]{
                attempt(AttemptType.MOCK, AttemptStatus.IN_PROGRESS),
                attempt(AttemptType.PLACEMENT, AttemptStatus.SUBMITTED)}) {
            when(attempts.findByIdAndUserId(attemptId, owner)).thenReturn(Optional.of(attempt));
            assertThatThrownBy(() -> useCase.execute(owner, attemptId)).isInstanceOf(InvalidAssessmentStateException.class);
        }
        verify(attempts, never()).save(any());
    }

    @Test
    void cancelledPlacementIsNoLongerReturnedAsCurrent() {
        AssessmentAttempt cancelled = attempt(AttemptType.PLACEMENT, AttemptStatus.CANCELLED);
        when(attempts.findByUserId(owner)).thenReturn(java.util.List.of(cancelled));
        assertThat(new GetCurrentPlacementAttemptUseCase(attempts).execute(owner)).isEmpty();
    }

    private AssessmentAttempt attempt(AttemptType type, AttemptStatus status) {
        Instant now = Instant.now();
        return new AssessmentAttempt(attemptId, owner, UUID.randomUUID(), type, AttemptMode.STANDARD,
                AttemptChannel.WEB, status, now, null, null, 0, now, now);
    }
}
