package com.ieltspath.learning.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Which essays the learner already asked a Mentor to review, and the id of that review in Assessment. */
public interface HumanReviewRequestRepository {
    Optional<UUID> findReviewId(UUID submissionId);

    /** The essay of the learner's review request, found by the review id Assessment holds; empty for anyone else's. */
    Optional<UUID> findSubmissionId(UUID reviewSubmissionId, UUID userId);

    /** Remembers the request; false when the essay already has one (the first request is kept). */
    boolean insert(UUID submissionId, UUID userId, UUID reviewSubmissionId);

    /** The learner's requests, newest first, one page: {@code offset} rows are skipped and at most {@code limit} returned. */
    List<Requested> findByUser(UUID userId, int limit, int offset);

    /** An essay asked to be reviewed, with enough of it to recognise it in a list. */
    record Requested(UUID submissionId, UUID reviewSubmissionId, String source, String task, String stem, int wordCount,
                     Instant requestedAt) {}
}
