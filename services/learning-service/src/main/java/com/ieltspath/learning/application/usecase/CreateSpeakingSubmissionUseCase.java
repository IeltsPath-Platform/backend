package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingTestQuestions;
import com.ieltspath.learning.application.service.SpeakingTestQuestions.Resolved;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Opens a Speaking submission, before any file is stored: the learner must have given consent and hold enough points
 * (only read here, the charge comes after grading), and have no other submission waiting. Sending the same
 * {@code requestId} again returns the submission it created.
 */
@Service
public class CreateSpeakingSubmissionUseCase {
    private final SpeakingSubmissionRepository submissions;
    private final SpeakingTestQuestions questions;
    private final AccessClient points;
    private final LearnerLock lock;
    private final SpeakingPracticeSettings settings;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public CreateSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions, SpeakingTestQuestions questions,
                                           AccessClient points, LearnerLock lock, SpeakingPracticeSettings settings,
                                           PlatformTransactionManager transactionManager) {
        this.submissions = submissions;
        this.questions = questions;
        this.points = points;
        this.lock = lock;
        this.settings = settings;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public SpeakingSubmissionResult execute(UUID userId, UUID requestId, UUID packageVersionId,
                                            List<UUID> partSectionIds, boolean consent) {
        if (!settings.enabled()) {
            throw new LearningRequestException(503, "SPEAKING_GRADING_DISABLED",
                    "AI Speaking grading is not available yet");
        }
        if (!consent) {
            throw new LearningRequestException(422, "CONSENT_REQUIRED", "Consent to process the recordings is required");
        }
        Optional<SpeakingSubmission> existing = submissions.findByRequestId(requestId);
        if (existing.isPresent()) return SpeakingSubmissionResult.from(replay(existing.get(), userId, packageVersionId));

        requireBalance();
        Resolved resolved = questions.resolve(packageVersionId, partSectionIds);
        SpeakingSubmission created = transaction.execute(status -> start(userId, requestId, packageVersionId, resolved));
        return SpeakingSubmissionResult.from(created);
    }

    private SpeakingSubmission start(UUID userId, UUID requestId, UUID packageVersionId, Resolved resolved) {
        lock.lock(userId);
        Optional<SpeakingSubmission> existing = submissions.findByRequestId(requestId);
        if (existing.isPresent()) return replay(existing.get(), userId, packageVersionId);

        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(settings.uploadStaleSeconds());
        for (SpeakingSubmission active : submissions.findActive(userId)) {
            if (active.isStaleUpload(cutoff)) {
                active.abandon(now);
                submissions.save(active);
            } else {
                throw new LearningRequestException(409, "SPEAKING_SUBMISSION_IN_PROGRESS",
                        "You already have a Speaking submission waiting to be graded", active.id());
            }
        }
        SpeakingSubmission submission = SpeakingSubmission.start(UUID.randomUUID(), userId, requestId, packageVersionId,
                resolved.scope(), settings.pointCost(), now, resolved.questions(), now);
        submissions.save(submission);
        return submission;
    }

    private static SpeakingSubmission replay(SpeakingSubmission existing, UUID userId, UUID packageVersionId) {
        if (!existing.userId().equals(userId) || !existing.packageVersionId().equals(packageVersionId)) {
            throw new LearningRequestException(409, "REQUEST_CONFLICT", "This request id was used for another submission");
        }
        return existing;
    }

    private void requireBalance() {
        long balance;
        try {
            balance = points.balance();
        } catch (AccessUnavailableException exception) {
            throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable");
        }
        if (balance < settings.pointCost()) {
            throw new LearningRequestException(402, "INSUFFICIENT_POINTS",
                    "Grading costs " + settings.pointCost() + " points");
        }
    }
}
