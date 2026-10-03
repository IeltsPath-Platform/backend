package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.entity.ReviewSet;
import com.group01.learning.domain.service.ReviewRule;
import com.group01.learning.domain.vo.ReviewStatus;
import com.group01.learning.domain.vo.LearningSkill;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A review of one weak knowledge point. While PENDING it has at most one open practice set. A passed set makes it
 * DONE; after {@link ReviewRule#MAX_FAILED_REVIEW_SETS} failed sets it is SKIPPED so the learner is never stuck, and it
 * is also SKIPPED when Content has no package left. DONE and SKIPPED are final.
 */
public final class ReviewItem {
    private final UUID id;
    private final UUID userId;
    private final UUID knowledgePointId;
    private final UUID lessonId;
    private final LearningSkill skill;
    private ReviewStatus status;
    private ReviewSet openSet;
    private int failedSets;
    private ReviewSet assignedSet;
    private ReviewSet answeredSet;

    private ReviewItem(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                       ReviewSet openSet, int failedSets, LearningSkill skill) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.knowledgePointId = Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId");
        this.skill = skill;
        this.status = Objects.requireNonNull(status, "status");
        this.openSet = openSet;
        this.failedSets = failedSets;
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets) {
        return restore(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, null);
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets, LearningSkill skill) {
        return new ReviewItem(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, skill);
    }

    /** Gives the learner a new practice set; only a PENDING review without an open set takes one. */
    public ReviewSet assignSet(UUID setId, UUID packageId, UUID packageVersionId) {
        requirePending();
        if (openSet != null) throw new IllegalStateException("Review already has an open set");
        openSet = new ReviewSet(setId, packageId, packageVersionId);
        assignedSet = openSet;
        return openSet;
    }

    /** Removes an unanswered set whose package was revealed by another submission. */
    public ReviewSet discardOpenSet() {
        requirePending();
        if (openSet == null) throw new IllegalStateException("Review has no open set");
        ReviewSet discarded = openSet;
        openSet = null;
        return discarded;
    }

    /** Content has no practice package left for the knowledge point: let the learner continue. */
    public void skip() {
        requirePending();
        status = ReviewStatus.SKIPPED;
    }

    /** Whether {@code setId} is this review's open set, the only one that may be answered. */
    public boolean acceptsAnswersFor(UUID setId) {
        return status == ReviewStatus.PENDING && openSet != null && openSet.id().equals(setId);
    }

    /** Records the answered open set and returns the review's new status. */
    public ReviewStatus recordSetResult(UUID setId, UUID requestId, boolean passed) {
        if (!acceptsAnswersFor(setId)) throw new IllegalStateException("Review set is closed");
        openSet.close(requestId, passed);
        answeredSet = openSet;
        openSet = null;
        if (passed) {
            status = ReviewStatus.DONE;
        } else if (++failedSets >= ReviewRule.MAX_FAILED_REVIEW_SETS) {
            status = ReviewStatus.SKIPPED;
        }
        return status;
    }

    private void requirePending() {
        if (status != ReviewStatus.PENDING) throw new IllegalStateException("Review is " + status);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID knowledgePointId() { return knowledgePointId; }
    public UUID lessonId() { return lessonId; }
    public LearningSkill skill() { return skill; }
    public ReviewStatus status() { return status; }
    public Optional<ReviewSet> openSet() { return Optional.ofNullable(openSet); }

    /** The set assigned since loading, for the repository to insert. */
    public Optional<ReviewSet> assignedSet() { return Optional.ofNullable(assignedSet); }

    /** The set answered since loading, for the repository to close. */
    public Optional<ReviewSet> answeredSet() { return Optional.ofNullable(answeredSet); }
}
