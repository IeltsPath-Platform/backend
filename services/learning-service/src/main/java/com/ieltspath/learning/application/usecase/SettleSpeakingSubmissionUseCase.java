package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.SpeakingPathUpdater;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Charges a graded Speaking submission and releases the grade to the learner. The background job stops at
 * {@code PAYMENT_PENDING} because only the learner may spend their points: the access service accepts a debit only from
 * the wallet's owner, and a job holds no learner token. So the debit runs here, in the learner's own request, once per
 * submission (the idempotency key makes a repeated call harmless): debit, then in one transaction {@code GRADED} and the
 * learning path update. Not enough points leaves the submission {@code PAYMENT_PENDING}, to be settled after a top-up.
 */
@Service
public class SettleSpeakingSubmissionUseCase {
    private final SpeakingSubmissionRepository submissions;
    private final AccessClient points;
    private final SpeakingPathUpdater pathUpdater;
    private final LearnerLock learnerLock;
    private final SpeakingPracticeSettings settings;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public SettleSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions, AccessClient points,
                                           SpeakingPathUpdater pathUpdater, LearnerLock learnerLock,
                                           SpeakingPracticeSettings settings, ObjectMapper json,
                                           PlatformTransactionManager transactionManager) {
        this.submissions = submissions;
        this.points = points;
        this.pathUpdater = pathUpdater;
        this.learnerLock = learnerLock;
        this.settings = settings;
        this.json = json;
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
            applyToPath(current);
            return current;
        });
        return SpeakingSubmissionResult.from(settled);
    }

    /** The criterion bands go to the learner's knowledge points, only when the signals say the speech can be trusted. */
    @SuppressWarnings("unchecked")
    private void applyToPath(SpeakingSubmission submission) {
        if (submission.rubricSnapshot() == null || submission.result() == null) return;
        LearningContentClient.Rubric rubric = json.convertValue(submission.rubricSnapshot(), LearningContentClient.Rubric.class);
        Map<String, Object> signals = (Map<String, Object>) submission.result().get("signals");
        pathUpdater.apply(submission.userId(), submission.id(), rubric, signals, BigDecimal.valueOf(settings.passBand()));
    }
}
