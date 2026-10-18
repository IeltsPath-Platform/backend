package com.ieltspath.learning.domain.repository;

import java.util.Optional;
import java.util.UUID;

/** Which essays the learner already asked a Mentor to review, and the id of that review in Assessment. */
public interface HumanReviewRequestRepository {
    Optional<UUID> findReviewId(UUID submissionId);

    /** Remembers the request; false when the essay already has one (the first request is kept). */
    boolean insert(UUID submissionId, UUID userId, UUID reviewSubmissionId);
}
