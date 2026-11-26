package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Charges a graded Speaking submission and releases the grade to the learner. The background job stops at
 * {@code PAYMENT_PENDING} because only the learner may spend points; a Premium submission has cost zero. The debit, if
 * needed, runs in the learner's own request once per submission (the idempotency key makes a repeated call harmless),
 * then one transaction moves it to {@code GRADED}. Not enough points leaves a paid submission {@code PAYMENT_PENDING}.
 * Speaking practice is outside the course, so a grade never reaches the learner's mastery.
 */
@Service
public class SettleSpeakingSubmissionUseCase {
    private static final Logger log = LoggerFactory.getLogger(SettleSpeakingSubmissionUseCase.class);
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
                        "Không tìm thấy bài nộp Speaking."));
        if (submission.status() != SpeakingSubmissionStatus.PAYMENT_PENDING) return SpeakingSubmissionResult.from(submission);

        UUID ledgerEntry = submission.pointCost() == 0 ? null : points.debit(userId, submission.pointCost(),
                "SPEAKING_AI", submission.id(), "speaking:" + userId + ":" + submission.requestId(),
                "AI Speaking practice grading");
        boolean[] released = {false};
        SpeakingSubmission settled = transaction.execute(status -> {
            learnerLock.lock(userId);
            SpeakingSubmission current = submissions.findForUpdate(submissionId).orElseThrow();
            if (current.status() != SpeakingSubmissionStatus.PAYMENT_PENDING) return current;
            current.graded(ledgerEntry, Instant.now(clock));
            submissions.save(current);
            released[0] = true;
            return current;
        });
        if (released[0]) logReleased(submission, settled);
        return SpeakingSubmissionResult.from(settled);
    }

    /**
     * The last server-side mark of the wait: {@code sinceCreatedMs} from the moment the learner's browser opened the
     * submission and {@code sinceQueuedMs} from the moment it was completed, up to the grade being released. {@code
     * waitedForLearnerMs} is how long the finished grade waited for the learner's page to ask for it (its polling interval).
     */
    private void logReleased(SpeakingSubmission pending, SpeakingSubmission settled) {
        Instant now = Instant.now(clock);
        log.info("Speaking grade released: submissionId={}, sinceCreatedMs={}, sinceQueuedMs={}, waitedForLearnerMs={}",
                settled.id(), millisSince(settled.createdAt(), now), millisSince(settled.queuedAt(), now),
                millisSince(pending.updatedAt(), now));
    }

    /** Milliseconds from {@code from} to {@code now}, or -1 when the moment is not known: a log line never breaks a request. */
    private static long millisSince(Instant from, Instant now) {
        return from == null ? -1 : Math.max(0, Duration.between(from, now).toMillis());
    }
}
