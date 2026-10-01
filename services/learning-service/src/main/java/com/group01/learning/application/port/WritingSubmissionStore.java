package com.group01.learning.application.port;

import com.group01.learning.application.writing.EssayPrompt;
import com.group01.learning.application.writing.WritingGrade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Essay submissions and the daily LLM usage counter. Moves into GRADING run under the learner's advisory lock;
 * moves out of GRADING are single conditional updates that need no lock.
 */
public interface WritingSubmissionStore {
    Optional<Submission> findByRequestId(UUID requestId);

    /** Row lock on the submission; requires a transaction. */
    Optional<Submission> findForUpdate(UUID id);

    Optional<Submission> findOwned(UUID id, UUID userId);

    /** GRADING rows of the block that started before {@code staleBefore} → FAILED ({@code GRADING_ABANDONED}). */
    void abandonStaleGrading(UUID userId, UUID blockId, Instant staleBefore);

    boolean hasGrading(UUID userId, UUID blockId);

    void insertGrading(NewSubmission submission);

    /** FAILED → GRADING starting at {@code now}; false when the row is in any other state. */
    boolean restartGrading(UUID id, Instant now);

    /** Counts one call when the day's count is below {@code limit}; false when the limit is reached. */
    boolean incrementDailyUsage(UUID userId, LocalDate day, String kind, int limit);

    /** GRADING → FAILED. */
    void markFailed(UUID id, String failureCode);

    /** GRADING → PAYMENT_PENDING with the stored grade; false when the row is no longer GRADING. */
    boolean saveGrade(UUID id, WritingGrade grade, boolean passed);

    /** Keeps PAYMENT_PENDING and records why the last debit failed. */
    void recordPaymentFailure(UUID id, String code);

    /** PAYMENT_PENDING → GRADED. */
    void markGraded(UUID id, UUID ledgerEntryId);

    /** Whether the learner has a GRADED, passed submission for the block, other than {@code excludeId}. */
    boolean blockPassed(UUID userId, UUID blockId, UUID excludeId);

    /** Newest submission and whether the block was ever passed, per essay block; one query. */
    Map<UUID, BlockSummary> summarizeBlocks(UUID userId, Collection<UUID> blockIds);

    record NewSubmission(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID requestId, String essayText,
                         int wordCount, EssayPrompt prompt, int pointCost, Instant now) {}

    record Submission(UUID id, UUID userId, UUID lessonId, UUID blockId, UUID requestId, String essayText,
                      int wordCount, EssayPrompt prompt, String status, int pointCost, UUID ledgerEntryId,
                      String failureCode, WritingGrade grade, BigDecimal overallBand, Boolean passed,
                      Instant gradingStartedAt) {}

    /** The grade of the newest submission is set only when it is GRADED, so a withheld grade never leaks. */
    record BlockSummary(UUID latestId, String latestStatus, BigDecimal latestOverallBand, Boolean latestPassed,
                        boolean passed) {}
}
