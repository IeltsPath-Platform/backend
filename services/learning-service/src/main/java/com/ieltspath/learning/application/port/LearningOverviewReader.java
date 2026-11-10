package com.ieltspath.learning.application.port;

import java.time.Instant;
import java.util.UUID;

/** Lifetime recorded activity, independent of curriculum metadata and progress pagination. */
public interface LearningOverviewReader {
    Totals totals(UUID userId);

    record Totals(Instant dataFrom, long studyMilliseconds, long completedLessons) {}
}
