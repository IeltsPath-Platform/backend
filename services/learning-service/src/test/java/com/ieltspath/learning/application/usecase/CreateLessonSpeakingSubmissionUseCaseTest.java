package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.PracticeOrigin;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateLessonSpeakingSubmissionUseCaseTest {
    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final SpeakingSubmissionRepository submissions = mock(SpeakingSubmissionRepository.class);
    private final PracticeAccess access = mock(PracticeAccess.class);
    private final CreateSpeakingSubmissionUseCase create = mock(CreateSpeakingSubmissionUseCase.class);
    private final CreateLessonSpeakingSubmissionUseCase useCase =
            new CreateLessonSpeakingSubmissionUseCase(attempts, submissions, access, create);
    private final UUID userId = UUID.randomUUID(), attemptId = UUID.randomUUID(), lessonId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID(), requestId = UUID.randomUUID();
    private PracticeAttempt attempt;

    @BeforeEach
    void setup() {
        attempt = mock(PracticeAttempt.class);
        when(attempts.findOwned(userId, attemptId)).thenReturn(Optional.of(attempt));
        when(attempt.origin()).thenReturn(PracticeOrigin.LESSON);
        when(attempt.lessonId()).thenReturn(lessonId);
        when(attempt.packageVersionId()).thenReturn(versionId);
        when(submissions.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(submissions.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.empty());
    }

    private static void rejects(Runnable call, int status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(LearningRequestException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(status);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }

    @Test
    void opensTheSubmissionWithTheLessonAndTheAttemptOfThePracticeSet() {
        useCase.execute(userId, attemptId, requestId, true);

        verify(access).require(userId, lessonId);
        verify(create).executeForAttempt(userId, requestId, versionId, lessonId, attemptId, true);
    }

    @Test
    void anAttemptThatIsNotTheLearnersIsNotFound() {
        when(attempts.findOwned(userId, attemptId)).thenReturn(Optional.empty());

        rejects(() -> useCase.execute(userId, attemptId, requestId, true), 404, "NOT_FOUND");
        verify(create, never()).executeForAttempt(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void aSubmittedAttemptTakesNoMoreAnswers() {
        when(attempt.submitted()).thenReturn(true);

        rejects(() -> useCase.execute(userId, attemptId, requestId, true), 409, "ATTEMPT_SUBMITTED");
    }

    @Test
    void freePracticeHasNoRecordedAnswer() {
        when(attempt.origin()).thenReturn(PracticeOrigin.FREE);

        rejects(() -> useCase.execute(userId, attemptId, requestId, true), 409, "FREE_PRACTICE_SPEAKING_UNSUPPORTED");
    }

    @Test
    void anAttemptThatAlreadyHasAGradedAnswerMustBeStartedAgain() {
        SpeakingSubmission graded = mock(SpeakingSubmission.class);
        when(graded.status()).thenReturn(SpeakingSubmissionStatus.GRADED);
        when(submissions.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.of(graded));

        rejects(() -> useCase.execute(userId, attemptId, requestId, true), 409, "SPEAKING_ALREADY_GRADED");
    }

    @Test
    void sendingTheSameRequestAgainReturnsTheSubmissionEvenOnceItIsGraded() {
        SpeakingSubmission graded = mock(SpeakingSubmission.class);
        when(graded.status()).thenReturn(SpeakingSubmissionStatus.GRADED);
        when(submissions.findLatestForAttempt(userId, attemptId)).thenReturn(Optional.of(graded));
        when(submissions.findByRequestId(requestId)).thenReturn(Optional.of(graded));

        useCase.execute(userId, attemptId, requestId, true);

        verify(create).executeForAttempt(userId, requestId, versionId, lessonId, attemptId, true);
    }
}
