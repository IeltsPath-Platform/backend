package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlacementHistoryReader {
    /** The learner's submitted placement attempts, newest first. */
    List<Item> find(UUID userId, long offset, int limit);

    /**
     * {@code graded} is false while no completed result exists, and {@code overallBand} is then null.
     * {@code pool} is null for an attempt made before exams were split into pools.
     */
    record Item(UUID attemptId, String pool, Instant submittedAt, boolean graded, Double overallBand,
                boolean partial) {}
}
