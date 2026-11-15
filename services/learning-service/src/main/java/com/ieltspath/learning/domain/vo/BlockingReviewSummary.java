package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The blocking review a learner has on one knowledge point, whatever has become of it: still PENDING, DONE after a passed
 * set, or SKIPPED when no package was left. Optional reviews are not summarised. {@code setsAnswered} counts the sets the
 * learner has submitted, and {@code lastCorrect} and {@code lastTotal} are the score of the latest of them (null before any).
 * {@code doneAt} is null while the review is PENDING.
 */
public record BlockingReviewSummary(UUID reviewId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                    Instant createdAt, Instant doneAt, int setsAnswered, Integer lastCorrect,
                                    Integer lastTotal) {
    public BlockingReviewSummary {
        Objects.requireNonNull(reviewId, "reviewId");
        Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        Objects.requireNonNull(status, "status");
    }
}
