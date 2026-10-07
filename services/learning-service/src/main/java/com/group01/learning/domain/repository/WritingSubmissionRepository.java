package com.group01.learning.domain.repository;

import com.group01.learning.domain.aggregate.WritingSubmission;
import com.group01.learning.domain.vo.WritingSubmissionStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface WritingSubmissionRepository {
    Optional<WritingSubmission> findByRequestId(UUID requestId);

    /** Row lock on the submission; requires a transaction. */
    Optional<WritingSubmission> findForUpdate(UUID id);

    Optional<WritingSubmission> findOwned(UUID id, UUID userId);

    /** The learner's GRADING submissions for the block. */
    List<WritingSubmission> findGrading(UUID userId, UUID blockId);

    /** The learner's GRADING submissions for one essay question of a practice attempt. */
    List<WritingSubmission> findGradingForPractice(UUID userId, UUID practiceAttemptId, UUID questionVersionId);

    /** The newest submission of each essay question of the attempt, keyed by question version. */
    Map<UUID, WritingSubmission> latestForPractice(UUID userId, UUID practiceAttemptId);

    /**
     * Inserts a new submission, or writes a loaded one only if it is still in {@link WritingSubmission#persistedStatus()};
     * false when another request changed it first. Moves out of GRADING happen without the learner's lock, so this
     * check is what keeps two requests from overwriting each other.
     */
    boolean save(WritingSubmission submission);

    /** Whether the learner has a GRADED, passed submission for the block, other than {@code excludeId}. */
    boolean blockPassed(UUID userId, UUID blockId, UUID excludeId);

    /** Newest submission and whether the block was ever passed, per essay block; one query. */
    Map<UUID, BlockSummary> summarizeBlocks(UUID userId, Collection<UUID> blockIds);

    /** The grade of the newest submission is set only when it is GRADED, so a withheld grade never leaks. */
    record BlockSummary(UUID latestId, WritingSubmissionStatus latestStatus, BigDecimal latestOverallBand,
                        Boolean latestPassed, boolean passed) {}
}
