package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.CreateAssessmentResultCommand;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.domain.repository.AssessmentResultRepository;
import com.group01.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAssessmentResultUseCaseTest {
    @Mock AssessmentAttemptRepository attempts;
    @Mock AssessmentResultRepository results;

    private final UUID userId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    @Test
    void creatingAResultOpensADraftThatIsNotFinal() {
        stubSubmittedAttempt();
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.empty());
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new CreateAssessmentResultUseCase(attempts, results)
                .execute(new CreateAssessmentResultCommand(userId, attemptId, 6.0));

        assertEquals(1, result.resultVersion());
        assertEquals(AssessmentResult.DRAFT, result.status());
        assertNull(result.completedAt());
    }

    @Test
    void regradeOpensTheNextVersionAfterTheLatestIsFinal() {
        stubSubmittedAttempt();
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(UUID.randomUUID(), attemptId, 1, AssessmentResult.COMPLETED, 6.0, Instant.now())));
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new CreateAssessmentResultUseCase(attempts, results)
                .execute(new CreateAssessmentResultCommand(userId, attemptId, 6.5));

        assertEquals(2, result.resultVersion());
        assertEquals(AssessmentResult.DRAFT, result.status());
    }

    @Test
    void anotherVersionCannotOpenWhileTheLatestIsStillBeingGraded() {
        stubSubmittedAttempt();
        when(results.findLatestByAttemptId(attemptId)).thenReturn(Optional.of(
                new AssessmentResult(UUID.randomUUID(), attemptId, 1, AssessmentResult.DRAFT, null, null)));

        assertThrows(InvalidAssessmentStateException.class, () -> new CreateAssessmentResultUseCase(attempts, results)
                .execute(new CreateAssessmentResultCommand(userId, attemptId, 6.5)));
        verify(results, never()).save(any());
    }

    private void stubSubmittedAttempt() {
        Instant now = Instant.now();
        when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(new AssessmentAttempt(attemptId,
                userId, UUID.randomUUID(), AttemptType.QUIZ, AttemptMode.STANDARD, AttemptChannel.WEB,
                AttemptStatus.SUBMITTED, now, now, null, 1, now, now, UUID.randomUUID())));
    }
}
