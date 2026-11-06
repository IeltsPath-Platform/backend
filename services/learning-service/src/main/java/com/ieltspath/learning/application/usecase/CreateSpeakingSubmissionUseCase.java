package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.service.SpeakingAvailability;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingTestQuestions;
import com.ieltspath.learning.application.service.SpeakingTestQuestions.Resolved;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
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
    private final SpeakingAvailability availability;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public CreateSpeakingSubmissionUseCase(SpeakingSubmissionRepository submissions, SpeakingTestQuestions questions,
                                           AccessClient points, LearnerLock lock, SpeakingPracticeSettings settings,
                                           SpeakingAvailability availability,
                                           PlatformTransactionManager transactionManager) {
        this.submissions = submissions;
        this.questions = questions;
        this.points = points;
        this.lock = lock;
        this.settings = settings;
        this.availability = availability;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public SpeakingSubmissionResult execute(UUID userId, UUID requestId, UUID packageVersionId,
                                            List<UUID> partSectionIds, boolean consent) {
        requireAvailable(consent);
        Optional<SpeakingSubmission> existing = submissions.findByRequestId(requestId);
        if (existing.isPresent()) return SpeakingSubmissionResult.from(replay(existing.get(), userId, packageVersionId, null));
        requireBalance();
        return open(userId, requestId, packageVersionId, questions.resolve(packageVersionId, partSectionIds), null, null);
    }

    /**
     * Opens the submission for a recorded answer inside a lesson's practice attempt. The caller has checked that the
     * attempt is the learner's, open and not free practice; the submission keeps the lesson and the attempt, which is
     * what lets its grade reach mastery when the attempt is submitted.
     */
    public SpeakingSubmissionResult executeForAttempt(UUID userId, UUID requestId, UUID packageVersionId, UUID lessonId,
                                                      UUID attemptId, boolean consent) {
        requireAvailable(consent);
        Optional<SpeakingSubmission> existing = submissions.findByRequestId(requestId);
        if (existing.isPresent()) {
            return SpeakingSubmissionResult.from(replay(existing.get(), userId, packageVersionId, attemptId));
        }
        requireBalance();
        return open(userId, requestId, packageVersionId, questions.resolveSetItems(packageVersionId), lessonId, attemptId);
    }

    private void requireAvailable(boolean consent) {
        if (!availability.isAvailable()) {
            throw new LearningRequestException(503, "SPEAKING_GRADING_DISABLED",
                    "AI Speaking grading is not available yet");
        }
        if (!consent) {
            throw new LearningRequestException(422, "CONSENT_REQUIRED", "Consent to process the recordings is required");
        }
    }

    private SpeakingSubmissionResult open(UUID userId, UUID requestId, UUID packageVersionId, Resolved resolved,
                                          UUID lessonId, UUID attemptId) {
        SpeakingSubmission created = transaction.execute(status -> start(userId, requestId, packageVersionId, resolved,
                lessonId, attemptId));
        return SpeakingSubmissionResult.from(created);
    }

    private SpeakingSubmission start(UUID userId, UUID requestId, UUID packageVersionId, Resolved resolved,
                                     UUID lessonId, UUID attemptId) {
        lock.lock(userId);
        Optional<SpeakingSubmission> existing = submissions.findByRequestId(requestId);
        if (existing.isPresent()) return replay(existing.get(), userId, packageVersionId, attemptId);

        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(settings.uploadStaleSeconds());
        for (SpeakingSubmission active : submissions.findActive(userId)) {
            // A file that was never sent for the same practice attempt (the page was reloaded, or storage failed) is
            // replaced at once; waiting for it to go stale would lock the learner out of the attempt for half an hour.
            boolean unsentForThisAttempt = attemptId != null && active.status() == SpeakingSubmissionStatus.UPLOADING
                    && attemptId.equals(active.practiceAttemptId());
            if (unsentForThisAttempt || active.isStaleUpload(cutoff)) {
                active.abandon(now);
                submissions.save(active);
            } else {
                throw new LearningRequestException(409, "SPEAKING_SUBMISSION_IN_PROGRESS",
                        "You already have a Speaking submission waiting to be graded", active.id());
            }
        }
        SpeakingSubmission submission = SpeakingSubmission.start(UUID.randomUUID(), userId, requestId, packageVersionId,
                resolved.scope(), settings.pointCost(), now, resolved.questions(), now);
        if (attemptId != null) submission.forPracticeAttempt(lessonId, attemptId);
        submissions.save(submission);
        return submission;
    }

    private static SpeakingSubmission replay(SpeakingSubmission existing, UUID userId, UUID packageVersionId,
                                             UUID attemptId) {
        if (!existing.userId().equals(userId) || !existing.packageVersionId().equals(packageVersionId)
                || !java.util.Objects.equals(existing.practiceAttemptId(), attemptId)) {
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
