package com.group01.learning.application.port;

import com.group01.learning.application.result.ReviewSubmissionResult;

import java.util.Optional;
import java.util.UUID;

/** The response of each answered review set, replayed for the same {@code requestId}. */
public interface ReviewSubmissionLog {
    Optional<Stored> find(UUID requestId);

    /** Stores the response of an answered set; the set row records the request and result first. */
    void save(UUID setId, ReviewSubmissionResult response);

    record Stored(UUID userId, UUID reviewId, UUID reviewSetId, ReviewSubmissionResult response) {}
}
