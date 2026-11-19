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
 * makes it DONE. A failed set keeps it PENDING and the learner gets a new set until one is passed; the first and third
 * failed sets send the learner back to THEORY. After the configured failure limit, a blocking review becomes optional.
 * The review is SKIPPED only when no
 * unrevealed package is left, so the learner is never stuck. DONE and SKIPPED are final.
 *
 * <p>An optional review ({@code blocking = false}, picked from the review basket or created from a free practice test)
 * never blocks anything and has no theory step: it is one set, DONE when passed and SKIPPED when failed. A suggestion
 * from Practice (optional, trigger {@code PRACTICE}) becomes blocking on its first failure only if it has not failed
 * before and has not previously been unblocked.
 */
public final class ReviewItem {
    private final UUID id;
    private final UUID userId;
    private final UUID knowledgePointId;
    private final UUID lessonId;
    private final LearningSkill skill;
    private final String triggerKind;
    private boolean blocking;
    private boolean unblocked;
    private boolean justUnblocked;
    private static final String PRACTICE_TRIGGER = "PRACTICE";
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
                       TheoryReason theoryReason, int theoryCompletedCount, String triggerKind, boolean blocking,
                       boolean unblocked) {
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
        this.blocking = blocking;
        this.unblocked = unblocked;
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
        return restore(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, skill, stage, theoryReason,
                theoryCompletedCount, triggerKind, true);
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets, LearningSkill skill, ReviewStage stage,
                                     TheoryReason theoryReason, int theoryCompletedCount, String triggerKind,
                                     boolean blocking) {
        return new ReviewItem(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, skill, stage,
                theoryReason, theoryCompletedCount, triggerKind, blocking, false);
    }

    public static ReviewItem restore(UUID id, UUID userId, UUID knowledgePointId, UUID lessonId, ReviewStatus status,
                                     ReviewSet openSet, int failedSets, LearningSkill skill, ReviewStage stage,
                                     TheoryReason theoryReason, int theoryCompletedCount, String triggerKind,
                                     boolean blocking, boolean unblocked) {
        return new ReviewItem(id, userId, knowledgePointId, lessonId, status, openSet, failedSets, skill, stage,
                theoryReason, theoryCompletedCount, triggerKind, blocking, unblocked);
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
        if (!blocking) throw new IllegalStateException("An optional review has no theory step");
        if (!isFresh()) throw new IllegalStateException("Review has already started");
        stage = ReviewStage.THEORY;
        theoryReason = Objects.requireNonNull(reason, "reason");
    }

    /** Whether {@code setId} is this review's open set, the only one that may be answered. */
    public boolean acceptsAnswersFor(UUID setId) {
        return status == ReviewStatus.PENDING && openSet != null && openSet.id().equals(setId);
    }

    /** Records the answered set and applies the blocking review's configured failed-set limit. */
    public ReviewStatus recordSetResult(UUID setId, UUID requestId, int correct, int total) {
        return recordSetResult(setId, requestId, correct, total, 4);
    }

    public ReviewStatus recordSetResult(UUID setId, UUID requestId, int correct, int total, int unblockAfterFailedSets) {
        if (!acceptsAnswersFor(setId)) throw new IllegalStateException("Review set is closed");
        // The third failed set sends the learner back to the theory, so the review cannot be released before the fourth.
        if (unblockAfterFailedSets < 4) throw new IllegalArgumentException("unblockAfterFailedSets must be at least 4");
        boolean passed = PassMark.passes(correct, total);
        openSet.close(requestId, correct, total, passed);
        answeredSet = openSet;
        openSet = null;
        if (passed) {
            status = ReviewStatus.DONE;
        } else {
            // A suggested review from Practice that fails its set is weak for real: it becomes blocking and reads the
            // theory first, like any blocking review after its first failed set.
            if (!blocking && PRACTICE_TRIGGER.equals(triggerKind) && failedSets == 0 && !unblocked) blocking = true;
            if (!blocking) {
                failedSets++;
                status = ReviewStatus.SKIPPED;
            } else {
                failedSets++;
                if (failedSets == 1 || failedSets == 3) {
                    stage = ReviewStage.THEORY;
                    theoryReason = failedSets == 3 ? TheoryReason.REPEATED_FAIL
                            : (double) correct / total < ReviewRule.LOW_SCORE ? TheoryReason.LOW_SCORE
                            : TheoryReason.SECOND_FAIL;
                } else if (failedSets >= unblockAfterFailedSets) {
                    blocking = false;
                    unblocked = true;
                    justUnblocked = true;
                    stage = ReviewStage.PRACTICE;
                    theoryReason = null;
                }
            }
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
    /** False for an optional review: it never locks a lesson, Practice or topic test. */
    public boolean blocking() { return blocking; }
    public boolean unblocked() { return unblocked; }
    /** True only for the transition that the repository must persist in this save. */
    public boolean justUnblocked() { return justUnblocked; }
    /** {@code PRACTICE}, {@code ASSESSMENT}, or null for reviews created before triggers were recorded. */
    public String triggerKind() { return triggerKind; }
    public Optional<ReviewSet> openSet() { return Optional.ofNullable(openSet); }

    /** The set assigned since loading, for the repository to insert. */
    public Optional<ReviewSet> assignedSet() { return Optional.ofNullable(assignedSet); }

    /** The set answered since loading, for the repository to close. */
    public Optional<ReviewSet> answeredSet() { return Optional.ofNullable(answeredSet); }
}
