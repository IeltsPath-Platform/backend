package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmitAssessmentAttemptUseCaseTest {
    @Test
    void repeatedSubmitDoesNotEnqueueGateEssayWorkTwice() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Instant now = Instant.now();
        AssessmentAttempt attempt = new AssessmentAttempt(attemptId, userId, UUID.randomUUID(),
                AttemptType.TOPIC_GATE, AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.IN_PROGRESS,
                now, null, null, 0, now, now);
        AssessmentAttemptRepository repository = mock(AssessmentAttemptRepository.class);
        EnqueueGateEssayGradingService gateGrading = mock(EnqueueGateEssayGradingService.class);
        when(repository.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));
        when(repository.save(attempt)).thenReturn(attempt);
        SubmitAssessmentAttemptUseCase submit = new SubmitAssessmentAttemptUseCase(repository, gateGrading);
        SubmitAssessmentAttemptCommand command = new SubmitAssessmentAttemptCommand(userId, attemptId);

        assertEquals(AttemptStatus.SUBMITTED, submit.execute(command).status());
        assertEquals(AttemptStatus.SUBMITTED, submit.execute(command).status());

        verify(repository, times(1)).save(attempt);
        verify(gateGrading, times(1)).enqueueOrGrade(attempt);
    }
}
