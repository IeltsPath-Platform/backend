package com.ieltspath.learning.application.port;

import java.time.LocalDate;
import java.util.UUID;

/** Per-learner daily count of LLM calls of one kind. */
public interface LlmUsageQuota {
    /** Counts one call when the day's count is below {@code limit}; false when the limit is reached. */
    boolean tryConsume(UUID userId, LocalDate day, String kind, int limit);

    /** Gives back one unit taken by {@link #tryConsume} when the call it paid for produced nothing. */
    void release(UUID userId, LocalDate day, String kind);
}
