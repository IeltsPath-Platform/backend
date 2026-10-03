package com.group01.learning.domain.repository;

import com.group01.learning.domain.aggregate.ReviewItem;
import com.group01.learning.domain.service.ReviewRule.ReviewCandidate;
import com.group01.learning.domain.vo.PendingReview;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.ReviewListEntry;
import com.group01.learning.domain.vo.ReviewStatus;
import com.group01.learning.domain.vo.PracticeReviewState;
import com.group01.learning.domain.vo.PracticeReviewCandidate;
import com.group01.learning.domain.vo.TopicReviewSnapshot;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /** Writes the review's status and the set it assigned or answered since loading. */
    void save(ReviewItem review);

    void deleteOpenSet(UUID userId, UUID reviewId, UUID setId);

    /** Packages the learner has been given in any review. */
    List<UUID> assignedPackageIds(UUID userId);

    /** Latest time each given package was assigned to the learner; packages never assigned are absent. */
    Map<UUID, Instant> lastAssignedAt(UUID userId, Collection<UUID> packageIds);
}
