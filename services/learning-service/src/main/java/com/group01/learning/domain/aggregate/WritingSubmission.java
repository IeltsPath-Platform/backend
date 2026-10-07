package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.vo.EssayPrompt;
import com.group01.learning.domain.vo.WritingGrade;
import com.group01.learning.domain.vo.WritingSubmissionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.group01.learning.domain.vo.WritingSubmissionStatus.*;

/**
 * One essay graded for a lesson block. The prompt is frozen with the essay so a resend grades the same question.
 * GRADING → PAYMENT_PENDING (graded) → GRADED (points debited); GRADING → FAILED; FAILED → GRADING (resend).
 * The grade stays withheld until GRADED. {@link #persistedStatus()} is the status last read or written, so the
 * repository can refuse a write when another request changed the submission first.
 */
public final class WritingSubmission {
    public static final String ABANDONED = "GRADING_ABANDONED";

    private final UUID id;
    private final UUID userId;
    private final UUID lessonId;
    private final UUID blockId;
    private final UUID practiceAttemptId;
    private final UUID requestId;
    private final String essayText;
    private final int wordCount;
    private final EssayPrompt prompt;
    private final int pointCost;
    private WritingSubmissionStatus status;
    private WritingSubmissionStatus persistedStatus;
    private String failureCode;
    private WritingGrade grade;
    private Boolean passed;
    private UUID ledgerEntryId;
    private Instant gradingStartedAt;

    private WritingSubmission(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID practiceAttemptId,
                              UUID requestId, String essayText,
                              int wordCount, EssayPrompt prompt, int pointCost, WritingSubmissionStatus status,
                              WritingSubmissionStatus persistedStatus, String failureCode, WritingGrade grade,
                              Boolean passed, UUID ledgerEntryId, Instant gradingStartedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId");
        if ((blockId == null) == (practiceAttemptId == null)) {
            throw new IllegalArgumentException("An essay belongs to a lesson block or to a practice attempt");
        }
        this.blockId = blockId;
        this.practiceAttemptId = practiceAttemptId;
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.essayText = Objects.requireNonNull(essayText, "essayText");
        this.wordCount = wordCount;
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.pointCost = pointCost;
        this.status = Objects.requireNonNull(status, "status");
        this.persistedStatus = persistedStatus;
        this.failureCode = failureCode;
        this.grade = grade;
        this.passed = passed;
        this.ledgerEntryId = ledgerEntryId;
        this.gradingStartedAt = gradingStartedAt;
    }

    /** A new submission, GRADING from {@code now}; not stored yet. */
    public static WritingSubmission start(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID requestId,
                                         String essayText, int wordCount, EssayPrompt prompt, int pointCost,
                                         Instant now) {
        return new WritingSubmission(id, userId, lessonId, Objects.requireNonNull(blockId, "blockId"), null,
                requestId, essayText, wordCount, prompt, pointCost, GRADING, null, null, null, null, null,
                Objects.requireNonNull(now, "now"));
    }

    /** A new essay for a question of a practice attempt, GRADING from {@code now}; not stored yet. */
    public static WritingSubmission startPractice(UUID id, UUID userId, UUID lessonId, UUID practiceAttemptId,
                                                 UUID requestId, String essayText, int wordCount, EssayPrompt prompt,
                                                 int pointCost, Instant now) {
        return new WritingSubmission(id, userId, lessonId, null, Objects.requireNonNull(practiceAttemptId,
                "practiceAttemptId"), requestId, essayText, wordCount, prompt, pointCost, GRADING, null, null, null,
                null, null, Objects.requireNonNull(now, "now"));
    }

    public static WritingSubmission restore(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID requestId,
                                           String essayText, int wordCount, EssayPrompt prompt, int pointCost,
                                           WritingSubmissionStatus status, String failureCode, WritingGrade grade,
                                           Boolean passed, UUID ledgerEntryId, Instant gradingStartedAt) {
        return restore(id, userId, lessonId, blockId, null, requestId, essayText, wordCount, prompt, pointCost, status,
                failureCode, grade, passed, ledgerEntryId, gradingStartedAt);
    }

    public static WritingSubmission restore(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID practiceAttemptId,
                                           UUID requestId, String essayText, int wordCount, EssayPrompt prompt,
                                           int pointCost, WritingSubmissionStatus status, String failureCode,
                                           WritingGrade grade, Boolean passed, UUID ledgerEntryId,
                                           Instant gradingStartedAt) {
        return new WritingSubmission(id, userId, lessonId, blockId, practiceAttemptId, requestId, essayText, wordCount,
                prompt, pointCost, status, status, failureCode, grade, passed, ledgerEntryId, gradingStartedAt);
    }

    /** A grading that started before {@code staleBefore} outlived its request (for example a crash). */
    public boolean isStale(Instant staleBefore) {
        return status == GRADING && gradingStartedAt.isBefore(staleBefore);
    }

    /** GRADING → FAILED ({@link #ABANDONED}), so the block can be graded again. */
    public void abandon() {
        fail(ABANDONED);
    }

    /** FAILED → GRADING: a resend grades the essay it was first sent with. */
    public void restart(Instant now) {
        require(FAILED);
        status = GRADING;
        failureCode = null;
        gradingStartedAt = Objects.requireNonNull(now, "now");
    }

    /** GRADING → FAILED; nothing is charged. */
    public void fail(String code) {
        require(GRADING);
        status = FAILED;
        failureCode = Objects.requireNonNull(code, "code");
    }

    /** GRADING → PAYMENT_PENDING with the grade withheld until payment; passes at the prompt's pass band. */
    public void recordGrade(WritingGrade grade) {
        require(GRADING);
        this.grade = Objects.requireNonNull(grade, "grade");
        this.passed = prompt.passBand() != null && grade.overallBand().compareTo(prompt.passBand()) >= 0;
        this.failureCode = null;
        status = PAYMENT_PENDING;
    }

    /** Stays PAYMENT_PENDING and records why the debit failed; a resend retries the debit only. */
    public void recordPaymentFailure(String code) {
        require(PAYMENT_PENDING);
        failureCode = Objects.requireNonNull(code, "code");
    }

    /** PAYMENT_PENDING → GRADED once the points are debited; the grade becomes visible. */
    public void markGraded(UUID ledgerEntryId) {
        require(PAYMENT_PENDING);
        this.ledgerEntryId = Objects.requireNonNull(ledgerEntryId, "ledgerEntryId");
        failureCode = null;
        status = GRADED;
    }

    /** Called by the repository after a successful write. */
    public void persisted() {
        persistedStatus = status;
    }

    private void require(WritingSubmissionStatus expected) {
        if (status != expected) throw new IllegalStateException("Writing submission is " + status + ", not " + expected);
    }

    public boolean isNew() { return persistedStatus == null; }
    public WritingSubmissionStatus persistedStatus() { return persistedStatus; }
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID lessonId() { return lessonId; }
    /** The lesson essay block; null for a practice essay. */
    public UUID blockId() { return blockId; }
    /** The practice attempt the essay answers; null for a lesson essay. */
    public UUID practiceAttemptId() { return practiceAttemptId; }
    public UUID requestId() { return requestId; }
    public String essayText() { return essayText; }
    public int wordCount() { return wordCount; }
    public EssayPrompt prompt() { return prompt; }
    public int pointCost() { return pointCost; }
    public WritingSubmissionStatus status() { return status; }
    public String failureCode() { return failureCode; }
    public WritingGrade grade() { return grade; }
    public BigDecimal overallBand() { return grade == null ? null : grade.overallBand(); }
    public Boolean passed() { return passed; }
    public UUID ledgerEntryId() { return ledgerEntryId; }
    public Instant gradingStartedAt() { return gradingStartedAt; }
}
