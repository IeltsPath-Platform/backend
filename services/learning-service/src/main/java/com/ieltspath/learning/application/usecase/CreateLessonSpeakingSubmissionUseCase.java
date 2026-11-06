package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.PracticeOrigin;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Opens a Speaking submission for the recorded answer of an open practice attempt of a lesson. Recording, grading and
 * the charge are those of free Speaking practice; what differs is that this submission keeps the lesson and the attempt,
 * so its grade reaches mastery when the attempt is submitted (and never before). One graded answer per attempt: to answer
 * again the learner starts a new attempt.
 */
@Service
public class CreateLessonSpeakingSubmissionUseCase {
    private final PracticeAttemptRepository attempts;
    private final SpeakingSubmissionRepository submissions;
    private final PracticeAccess access;
    private final CreateSpeakingSubmissionUseCase create;

    public CreateLessonSpeakingSubmissionUseCase(PracticeAttemptRepository attempts,
                                                 SpeakingSubmissionRepository submissions, PracticeAccess access,
                                                 CreateSpeakingSubmissionUseCase create) {
        this.attempts = attempts;
        this.submissions = submissions;
        this.access = access;
        this.create = create;
    }

    public SpeakingSubmissionResult execute(UUID userId, UUID attemptId, UUID requestId, boolean consent) {
        PracticeAttempt attempt = attempts.findOwned(userId, attemptId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));
        if (attempt.submitted()) {
            throw new LearningRequestException(409, "ATTEMPT_SUBMITTED", "The practice attempt was already submitted");
        }
        if (attempt.origin() == PracticeOrigin.FREE) {
            throw new LearningRequestException(409, "FREE_PRACTICE_SPEAKING_UNSUPPORTED",
                    "Free practice does not take recorded answers here");
        }
        access.require(userId, attempt.lessonId());
        boolean replay = submissions.findByRequestId(requestId).isPresent();
        if (!replay && submissions.findLatestForAttempt(userId, attemptId)
                .filter(latest -> latest.status() == SpeakingSubmissionStatus.GRADED).isPresent()) {
            throw new LearningRequestException(409, "SPEAKING_ALREADY_GRADED",
                    "This attempt already has a graded answer; start a new attempt to answer again");
        }
        return create.executeForAttempt(userId, requestId, attempt.packageVersionId(), attempt.lessonId(), attemptId,
                consent);
    }
}
