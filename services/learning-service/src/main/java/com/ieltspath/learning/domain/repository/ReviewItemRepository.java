package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.ReviewListEntry;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.PracticeReviewState;
import com.ieltspath.learning.domain.vo.PracticeReviewCandidate;
import com.ieltspath.learning.domain.vo.TopicReviewSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewItemRepository {
    /** The learner's review with its open set and failed set count; empty for another learner's review. */
    Optional<ReviewItem> findOwned(UUID userId, UUID reviewId);

    List<PendingReview> findPending(UUID userId);

    List<PracticeReviewState> findPracticeByLessons(UUID userId, Collection<UUID> lessonIds);

    TopicReviewSnapshot findForTopic(UUID userId, Collection<UUID> lessonIds);

    void insertPracticePending(UUID userId, List<PracticeReviewCandidate> candidates);

    List<ReviewListEntry> list(UUID userId, ReviewStatus status, LearningSkill skill, int limit);

    void backfillMissingSkill(UUID userId);

    /** Adds PENDING reviews; a knowledge point that already has one keeps it. */
    void insertPending(UUID userId, List<ReviewCandidate> candidates);

    /**
     * As {@link #insertPending(UUID, List)} with the given kind (for example {@code FREE_PRACTICE_TEST}); when
     * {@code blocking} is false the reviews are optional.
     */
    void insertPending(UUID userId, List<ReviewCandidate> candidates, boolean blocking, String triggerKind);

    /**
     * Makes the learner's optional PENDING reviews of these knowledge points blocking, with the new trigger. A
     * knowledge point can hold only one PENDING review, so a wrong answer in the main flow upgrades the optional one.
     */
    void promoteToBlocking(UUID userId, Collection<UUID> knowledgePointIds, String triggerKind);

    /** Starts an optional PENDING review; false when the knowledge point already has a PENDING review. */
    boolean insertOptional(UUID userId, UUID reviewId, UUID knowledgePointId, UUID lessonId, LearningSkill skill,
                           String triggerKind);

    /** Writes the review's status and the set it assigned or answered since loading. */
    void save(ReviewItem review);

    void deleteOpenSet(UUID userId, UUID reviewId, UUID setId);

    /** Packages the learner has been given in any review. */
    List<UUID> assignedPackageIds(UUID userId);
}
