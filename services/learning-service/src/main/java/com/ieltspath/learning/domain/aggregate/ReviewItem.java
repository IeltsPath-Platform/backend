package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.entity.ReviewSet;
import com.ieltspath.learning.domain.service.PassMark;
import com.ieltspath.learning.domain.service.ReviewRule;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TheoryReason;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A review of one weak knowledge point. While PENDING it is at a stage: PRACTICE gives at most one open set, THEORY
 * asks the learner to read the knowledge point's theory and answer a quick check before the next set. A passed set
 * makes it DONE; a failed set sends it to THEORY, and after {@link ReviewRule#MAX_FAILED_REVIEW_SETS} failed sets it is
 * SKIPPED so the learner is never stuck. It is also SKIPPED when no unrevealed package is left. DONE and SKIPPED are
 * final.
 */
public final class ReviewItem {
    private final UUID id;
    private final UUID userId;
    private final UUID knowledgePointId;
    private final UUID lessonId;
    private final LearningSkill skill;
    private final String triggerKind;
    private ReviewStatus status;
    private ReviewStage stage;
    private TheoryReason theoryReason;
    private int theoryCompletedCount;
    private ReviewSet openSet;
    private int failedSets;
    private ReviewSet assignedSet;
    private ReviewSet answeredSet;

    private ReviewItem(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                       ReviewSet openSet, int failedSets, LearningSkill skill, ReviewStage stage,
                       TheoryReason theoryReason, int theoryCompletedCount, String triggerKind) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.knowledgePointId = Objects.requireNonNull(knowledgePointId, "knowledgePointId");
        this.lessonId = Objects.requireNonNull(lessonId, "lessonId");
        this.skill = skill;
        this.status = Objects.requireNonNull(status, "status");
        this.openSet = openSet;
        this.failedSets = failedSets;
        this.stage = Objects.requireNonNull(stage, "stage");
        this.theoryReason = theoryReason;
        this.theoryCompletedCount = theoryCompletedCount;
        this.triggerKind = triggerKind;
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets) {
        return restore(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, null);
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets, LearningSkill skill) {
        return restore(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, skill,
                ReviewStage.PRACTICE, null, 0, null);
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets, LearningSkill skill, ReviewStage stage,
                                     TheoryReason theoryReason, int theoryCompletedCount, String triggerKind) {
        return new ReviewItem(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, skill, stage,
                theoryReason, theoryCompletedCount, triggerKind);
    }

    /** Gives the learner a new practice set; only a PENDING review at PRACTICE without an open set takes one. */
    public ReviewSet assignSet(UUID setId, UUID packageId, UUID packageVersionId) {
        requirePending();
        if (stage != ReviewStage.PRACTICE) throw new IllegalStateException("Review is at the theory stage");
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

    /** No unrevealed package is left for the knowledge point: let the learner continue. */
    public void skip() {
        requirePending();
        status = ReviewStatus.SKIPPED;
    }

    /** True while the review has not given a set or theory yet, so its starting stage can still be decided. */
    public boolean isFresh() {
        return status == ReviewStatus.PENDING && stage == ReviewStage.PRACTICE && openSet == null && failedSets == 0
                && theoryCompletedCount == 0;
    }

    /** Sends a fresh review to the theory before its first set (knowledge point already wrong in the lesson). */
    public void startWithTheory(TheoryReason reason) {
        if (!isFresh()) throw new IllegalStateException("Review has already started");
        stage = ReviewStage.THEORY;
        theoryReason = Objects.requireNonNull(reason, "reason");
    }

    /** Whether {@code setId} is this review's open set, the only one that may be answered. */
    public boolean acceptsAnswersFor(UUID setId) {
        return status == ReviewStatus.PENDING && openSet != null && openSet.id().equals(setId);
    }

    /** Records the answered open set and returns the review's new status. */
    public ReviewStatus recordSetResult(UUID setId, UUID requestId, int correct, int total) {
        if (!acceptsAnswersFor(setId)) throw new IllegalStateException("Review set is closed");
        boolean passed = PassMark.passes(correct, total);
        openSet.close(requestId, correct, total, passed);
        answeredSet = openSet;
        openSet = null;
        if (passed) {
            status = ReviewStatus.DONE;
        } else if (++failedSets >= ReviewRule.MAX_FAILED_REVIEW_SETS) {
            status = ReviewStatus.SKIPPED;
        } else {
            stage = ReviewStage.THEORY;
            theoryReason = (double) correct / total < ReviewRule.LOW_SCORE ? TheoryReason.LOW_SCORE
                    : TheoryReason.SECOND_FAIL;
        }
        return status;
    }

    /** The learner read the theory and answered the quick check (right or wrong): back to practice. */
    public void completeTheory() {
        requirePending();
        if (stage != ReviewStage.THEORY) throw new IllegalStateException("Review is not at the theory stage");
        stage = ReviewStage.PRACTICE;
        theoryCompletedCount++;
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
    public ReviewStage stage() { return stage; }
    public TheoryReason theoryReason() { return theoryReason; }
    public int theoryCompletedCount() { return theoryCompletedCount; }
    public int failedSets() { return failedSets; }
    /** {@code PRACTICE}, {@code ASSESSMENT}, or null for reviews created before triggers were recorded. */
    public String triggerKind() { return triggerKind; }
    public Optional<ReviewSet> openSet() { return Optional.ofNullable(openSet); }

    /** The set assigned since loading, for the repository to insert. */
    public Optional<ReviewSet> assignedSet() { return Optional.ofNullable(assignedSet); }

    /** The set answered since loading, for the repository to close. */
    public Optional<ReviewSet> answeredSet() { return Optional.ofNullable(answeredSet); }
}
