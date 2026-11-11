package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.exception.WritingGradingException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LlmUsageQuota;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.service.WritingScore;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The paid essay grading steps shared by lesson and practice essays: validate the text, check the balance, take the
 * daily LLM quota, grade outside any transaction, store the grade as PAYMENT_PENDING, then debit once (idempotent on
 * the ledger key) before the caller records GRADED. Each caller owns its gate, the GRADING row and what GRADED means.
 */
@Component
public class EssaySubmissionFlow {
    public static final String USAGE_KIND = "writing_grading";
    private static final Set<String> QUOTA_RELEASE_CODES = Set.of(
            "LLM_UNAVAILABLE", "AI_BUSY", "RUBRIC_UNAVAILABLE", "PROMPT_VERSION_UNAVAILABLE");
    private static final Logger log = LoggerFactory.getLogger(EssaySubmissionFlow.class);

    private final WritingSubmissionRepository essays;
    private final LlmUsageQuota quota;
    private final EssayGrader grader;
    private final AccessClient points;
    private final WritingSettings settings;
    private final MeterRegistry metrics;
    private final Clock clock = Clock.systemUTC();

    @Autowired
    public EssaySubmissionFlow(WritingSubmissionRepository essays, LlmUsageQuota quota, EssayGrader grader,
                               AccessClient points, WritingSettings settings, MeterRegistry metrics) {
        this.essays = essays;
        this.quota = quota;
        this.grader = grader;
        this.points = points;
        this.settings = settings;
        this.metrics = metrics;
    }

    /** The word count of a valid essay. */
    public int validate(String essayText) {
        if (essayText == null || essayText.isBlank()) {
            throw new LearningRequestException(422, "ESSAY_EMPTY", "The essay is empty");
        }
        if (essayText.length() > settings.maxChars()) throw essayTooLong();
        int words = WritingScore.countWords(essayText);
        if (words < settings.minWords()) {
            throw new LearningRequestException(422, "ESSAY_TOO_SHORT",
                    "The essay needs at least " + settings.minWords() + " words");
        }
        if (words > settings.maxWords()) throw essayTooLong();
        return words;
    }

    public void requireGraderAvailable() {
        if (!grader.available()) throw gradingUnavailable(null);
    }

    public void requireBalance() {
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

    /** Daily quota, then the LLM grade stored as PAYMENT_PENDING; no transaction or lock is held. */
    public void grade(WritingSubmission grading) {
        LocalDate quotaDay = LocalDate.now(clock.withZone(ZoneId.of(settings.quotaTimezone())));
        if (!quota.tryConsume(grading.userId(), quotaDay, USAGE_KIND, settings.dailyGradingLimit())) {
            grading.fail("DAILY_LIMIT_REACHED");
            essays.save(grading);
            recordOutcome("DAILY_LIMIT_REACHED", grading);
            throw new LearningRequestException(429, "DAILY_LIMIT_REACHED", "Daily grading limit reached");
        }
        EssayGrader.GradedEssay graded;
        try {
            graded = grader.grade(grading.prompt(), grading.essayText());
        } catch (WritingGradingException exception) {
            grading.fail(exception.getCode());
            essays.save(grading);
            log.warn("Essay grading failed: submissionId={}, code={}", grading.id(), exception.getCode());
            if (QUOTA_RELEASE_CODES.contains(exception.getCode())) releaseQuota(grading.userId(), quotaDay);
            recordOutcome(exception.getCode(), grading);
            throw gradingUnavailable(grading.id());
        } catch (LearningRequestException exception) {
            if (!"AI_BUSY".equals(exception.getCode())) throw exception;
            grading.fail("AI_BUSY");
            essays.save(grading);
            log.warn("Essay grading failed: submissionId={}, code=AI_BUSY", grading.id());
            releaseQuota(grading.userId(), quotaDay);
            recordOutcome("AI_BUSY", grading);
            throw exception;
        }
        grading.recordGrade(graded.grade(), graded.promptVersion());
        // False when the grading was abandoned and taken over by another request meanwhile.
        if (!essays.save(grading)) throw gradingInProgress();
        recordOutcome("GRADED", grading);
        DistributionSummary.builder("learning.writing.corrections")
                .register(metrics).record(graded.grade().corrections().size());
    }

    /**
     * Debits the grade once, then lets {@code finish} record GRADED with the ledger entry; resending a PAYMENT_PENDING
     * submission starts here.
     */
    public WritingSubmissionResult charge(WritingSubmission submission, String ledgerKey, String description,
                                          BiFunction<WritingSubmission, UUID, WritingSubmissionResult> finish) {
        UUID ledgerEntryId;
        try {
            ledgerEntryId = points.debit(submission.userId(), submission.pointCost(), submission.id(), ledgerKey,
                    description);
        } catch (InsufficientPointsException exception) {
            submission.recordPaymentFailure("INSUFFICIENT_POINTS");
            essays.save(submission);
            throw new LearningRequestException(402, "INSUFFICIENT_POINTS", "Not enough points to release the grade",
                    submission.id());
        } catch (AccessUnavailableException exception) {
            submission.recordPaymentFailure("PAYMENT_UNAVAILABLE");
            essays.save(submission);
            throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable; resend later",
                    submission.id());
        }
        return finish.apply(submission, ledgerEntryId);
    }

    public int pointCost() {
        return settings.pointCost();
    }

    public long staleGradingSeconds() {
        return settings.staleGradingSeconds();
    }

    private void releaseQuota(UUID userId, LocalDate day) {
        try {
            quota.release(userId, day, USAGE_KIND);
        } catch (RuntimeException exception) {
            log.warn("Writing quota could not be given back: reason={}", exception.getClass().getSimpleName());
        }
    }

    private void recordOutcome(String outcome, WritingSubmission submission) {
        String source = submission.isFree() ? "free"
                : submission.practiceAttemptId() == null ? "lesson" : "practice";
        metrics.counter("learning.writing.grading", "outcome", outcome, "source", source).increment();
    }

    private static LearningRequestException essayTooLong() {
        return new LearningRequestException(422, "ESSAY_TOO_LONG", "The essay is too long");
    }

    public static LearningRequestException gradingUnavailable(UUID submissionId) {
        return new LearningRequestException(503, "GRADING_UNAVAILABLE", "Grading is unavailable; nothing was charged",
                submissionId);
    }

    public static LearningRequestException gradingInProgress() {
        return new LearningRequestException(409, "GRADING_IN_PROGRESS", "This essay block is already being graded");
    }

    public static LearningRequestException requestConflict() {
        return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
    }
}
