package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Charges a graded Speaking submission and releases the grade to the learner. The background job stops at
 * {@code PAYMENT_PENDING} because only the learner may spend their points: the access service accepts a debit only from
 * the wallet's owner, and a job holds no learner token. So the debit runs here, in the learner's own request, once per
 * submission (the idempotency key makes a repeated call harmless): debit, then in one transaction {@code GRADED}.
 * Not enough points leaves the submission {@code PAYMENT_PENDING}, to be settled after a top-up. Speaking practice is
 * outside the course, so a grade never reaches the learner's mastery.
 */
@Service
public class SettleSpeakingSubmissionUseCase {
    private final SpeakingSubmissionRepository submissions;
    private final AccessClient points;
    private final LearnerLock learnerLock;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public SettleSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions, AccessClient points,
                                           LearnerLock learnerLock, PlatformTransactionManager transactionManager) {
        this.submissions = submissions;
        this.points = points;
        this.learnerLock = learnerLock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * @throws com.ieltspath.learning.application.exception.InsufficientPointsException (402) when the balance is short
     * @throws com.ieltspath.learning.application.exception.AccessUnavailableException (503) when points cannot be reached
     */
    public SpeakingSubmissionResult execute(UUID userId, UUID submissionId) {
        SpeakingSubmission submission = submissions.findOwned(submissionId, userId)
                .orElseThrow(() -> new LearningRequestException(404, "SPEAKING_SUBMISSION_NOT_FOUND",
                        "Speaking submission not found"));
        if (submission.status() != SpeakingSubmissionStatus.PAYMENT_PENDING) return SpeakingSubmissionResult.from(submission);

        UUID ledgerEntry = points.debit(userId, submission.pointCost(), "SPEAKING_AI", submission.id(),
                "speaking:" + userId + ":" + submission.requestId(), "AI Speaking practice grading");
        SpeakingSubmission settled = transaction.execute(status -> {
            learnerLock.lock(userId);
            SpeakingSubmission current = submissions.findForUpdate(submissionId).orElseThrow();
            if (current.status() != SpeakingSubmissionStatus.PAYMENT_PENDING) return current;
            current.graded(ledgerEntry, Instant.now(clock));
            submissions.save(current);
            return current;
        });
        return SpeakingSubmissionResult.from(settled);
    }
}
