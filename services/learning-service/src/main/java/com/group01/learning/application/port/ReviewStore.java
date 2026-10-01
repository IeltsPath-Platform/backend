package com.group01.learning.application.port;

import com.group01.learning.application.result.ReviewSubmissionResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Review sets and final-test assignments; callers hold the user's advisory lock. */
public interface ReviewStore {
    Optional<ReviewItem> findReview(UUID userId, UUID reviewId);

    Optional<ReviewSet> findOpenSet(UUID reviewId);

    /** The open set matching {@code setId}, locked for update; empty when it is closed or not this review's. */
    Optional<ReviewSet> lockOpenSet(UUID userId, UUID reviewId, UUID setId);

    List<UUID> assignedPackageIds(UUID userId);

    /** Latest assignment time of each given package for the user; packages never assigned are absent. */
    Map<UUID, Instant> lastAssignedAt(UUID userId, Collection<UUID> packageIds);

    UUID insertSet(UUID userId, UUID reviewId, UUID packageId, UUID packageVersionId);

    Optional<StoredReviewSubmission> findSubmission(UUID requestId);

    void closeSet(UUID setId, UUID requestId, boolean passed, ReviewSubmissionResult response);

    int failedSetCount(UUID reviewId);

    /** Moves a PENDING review to DONE or SKIPPED. */
    void finishReview(UUID reviewId, String status);

    Optional<TestAssignment> findOpenAssignment(UUID userId, UUID topicId);

    /** Latest consumption time of each package the user has used for the topic. */
    Map<UUID, Instant> lastConsumedAt(UUID userId, UUID topicId);

    TestAssignment insertAssignment(UUID userId, UUID topicId, UUID packageId, UUID packageVersionId);

    record ReviewItem(UUID reviewId, UUID lessonId, UUID knowledgePointId, String status) {}
    record ReviewSet(UUID reviewSetId, UUID reviewId, UUID packageId, UUID packageVersionId) {}
    record StoredReviewSubmission(UUID userId, UUID reviewId, UUID reviewSetId, ReviewSubmissionResult response) {}
    record TestAssignment(UUID assignmentId, UUID packageId, UUID packageVersionId) {}
}
