package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.ieltspath.learning.domain.vo.WritingSubmissionStatus.*;

/**
 * One essay graded for a lesson block. The prompt is frozen with the essay so a resend grades the same question.
 * GRADING → PAYMENT_PENDING (graded) → GRADED (points debited); GRADING → FAILED; FAILED → GRADING (resend). A free
 * essay sent to an examiner is AWAITING_EXAMINER from the start and stays so: the grade lives in Assessment.
 * The grade stays withheld until GRADED. {@link #persistedStatus()} is the status last read or written, so the
 * repository can refuse a write when another request changed the submission first.
 */
public final class WritingSubmission {
    public static final String ABANDONED = "GRADING_ABANDONED";
    /** The first essay is 1; three revisions make it 4. */
    public static final int MAX_REVISION_NO = 4;

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
    private String promptVersion;
    private Boolean passed;
    private UUID ledgerEntryId;
    private Instant gradingStartedAt;
    private UUID previousSubmissionId;
    private int revisionNo = 1;

    private WritingSubmission(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID practiceAttemptId,
                              UUID requestId, String essayText,
                              int wordCount, EssayPrompt prompt, int pointCost, WritingSubmissionStatus status,
                              WritingSubmissionStatus persistedStatus, String failureCode, WritingGrade grade,
                              String promptVersion, Boolean passed, UUID ledgerEntryId, Instant gradingStartedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.lessonId = lessonId;
        boolean free = lessonId == null && blockId == null && practiceAttemptId == null;
        if (!free && (lessonId == null || (blockId == null) == (practiceAttemptId == null))) {
            throw new IllegalArgumentException(
                    "An essay belongs to a lesson block, to a practice attempt, or to no lesson at all (free writing)");
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
        this.promptVersion = promptVersion;
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
                null, Objects.requireNonNull(now, "now"));
    }

    /** A new essay for a question of a practice attempt, GRADING from {@code now}; not stored yet. */
    public static WritingSubmission startPractice(UUID id, UUID userId, UUID lessonId, UUID practiceAttemptId,
                                                 UUID requestId, String essayText, int wordCount, EssayPrompt prompt,
                                                 int pointCost, Instant now) {
        return new WritingSubmission(id, userId, lessonId, null, Objects.requireNonNull(practiceAttemptId,
                "practiceAttemptId"), requestId, essayText, wordCount, prompt, pointCost, GRADING, null, null, null,
                null, null, null, Objects.requireNonNull(now, "now"));
    }

    /** A new free-writing essay, answering a prompt of the library outside any lesson; GRADING from {@code now}. */
    public static WritingSubmission startFree(UUID id, UUID userId, UUID requestId, String essayText, int wordCount,
                                              EssayPrompt prompt, int pointCost, Instant now) {
        return new WritingSubmission(id, userId, null, null, null, requestId, essayText, wordCount, prompt, pointCost,
                GRADING, null, null, null, null, null, null, Objects.requireNonNull(now, "now"));
    }

    /**
     * A free-writing essay the learner sent to an examiner instead of the AI: AWAITING_EXAMINER from {@code now}, nothing
     * to grade or charge here (the examiner's credit is spent in Access). Not stored yet.
     */
    public static WritingSubmission startFreeForExaminer(UUID id, UUID userId, UUID requestId, String essayText,
                                                         int wordCount, EssayPrompt prompt, Instant now) {
        return new WritingSubmission(id, userId, null, null, null, requestId, essayText, wordCount, prompt, 0,
                AWAITING_EXAMINER, null, null, null, null, null, null, Objects.requireNonNull(now, "now"));
    }

    public static WritingSubmission restore(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID requestId,
                                           String essayText, int wordCount, EssayPrompt prompt, int pointCost,
                                           WritingSubmissionStatus status, String failureCode, WritingGrade grade,
                                           Boolean passed, UUID ledgerEntryId, Instant gradingStartedAt) {
        return restore(id, userId, lessonId, blockId, null, requestId, essayText, wordCount, prompt, pointCost, status,
                failureCode, grade, null, passed, ledgerEntryId, gradingStartedAt);
    }

    public static WritingSubmission restore(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID practiceAttemptId,
                                           UUID requestId, String essayText, int wordCount, EssayPrompt prompt,
                                           int pointCost, WritingSubmissionStatus status, String failureCode,
                                           WritingGrade grade, Boolean passed, UUID ledgerEntryId,
                                           Instant gradingStartedAt) {
        return restore(id, userId, lessonId, blockId, practiceAttemptId, requestId, essayText, wordCount, prompt,
                pointCost, status, failureCode, grade, null, passed, ledgerEntryId, gradingStartedAt);
    }

    public static WritingSubmission restore(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID practiceAttemptId,
                                           UUID requestId, String essayText, int wordCount, EssayPrompt prompt,
                                           int pointCost, WritingSubmissionStatus status, String failureCode,
                                           WritingGrade grade, String promptVersion, Boolean passed,
                                           UUID ledgerEntryId, Instant gradingStartedAt) {
        return new WritingSubmission(id, userId, lessonId, blockId, practiceAttemptId, requestId, essayText, wordCount,
                prompt, pointCost, status, status, failureCode, grade, promptVersion, passed, ledgerEntryId,
                gradingStartedAt);
    }

    /**
     * Makes this essay the revision of {@code previous}; call before it is stored, or while restoring a stored one.
     * A revision starts from the newest graded essay of a lesson block and a learner revises three times at most.
     */
    public WritingSubmission revising(UUID previousSubmissionId, int revisionNo) {
        this.previousSubmissionId = Objects.requireNonNull(previousSubmissionId, "previousSubmissionId");
        if (revisionNo < 2 || revisionNo > MAX_REVISION_NO) {
            throw new IllegalArgumentException("revisionNo must be between 2 and " + MAX_REVISION_NO);
        }
        this.revisionNo = revisionNo;
        return this;
    }

    /** The revision number this essay's own revision would take; empty when it cannot be revised (see rules above). */
    public java.util.OptionalInt nextRevisionNo() {
        boolean revisable = status == GRADED && blockId != null && revisionNo < MAX_REVISION_NO;
        return revisable ? java.util.OptionalInt.of(revisionNo + 1) : java.util.OptionalInt.empty();
    }

    /** True when the learner has used all their revisions of this essay. */
    public boolean revisionLimitReached() {
        return revisionNo >= MAX_REVISION_NO;
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
        recordGrade(grade, null);
    }

    public void recordGrade(WritingGrade grade, String promptVersion) {
        require(GRADING);
        this.grade = Objects.requireNonNull(grade, "grade");
        this.promptVersion = promptVersion;
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
    /** The lesson of the essay; null for free writing. */
    public UUID lessonId() { return lessonId; }
    /** An essay written for a library prompt, owned by no lesson, block or practice attempt. */
    public boolean isFree() { return lessonId == null; }
    /** The lesson essay block; null for a practice or free essay. */
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
    public String promptVersion() { return promptVersion; }
    public BigDecimal overallBand() { return grade == null ? null : grade.overallBand(); }
    public Boolean passed() { return passed; }
    public UUID ledgerEntryId() { return ledgerEntryId; }
    public Instant gradingStartedAt() { return gradingStartedAt; }
    /** The graded essay this one revises; null for a first essay. */
    public UUID previousSubmissionId() { return previousSubmissionId; }
    public int revisionNo() { return revisionNo; }
}
