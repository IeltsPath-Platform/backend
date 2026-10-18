package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Supplementary work the learner is asked to do: after a first topic test below the mid-course line
 * ({@code MID_COURSE}, with the topic) or after a course not passed ({@code POST_COURSE}). The work is the blocking
 * reviews in {@code reviewIds}; the flag is {@code open} while any of them is still PENDING.
 */
public record RemediationFlag(UUID id, UUID userId, UUID courseId, UUID topicId, Kind kind, UUID sourceAttemptId,
                              List<UUID> reviewIds, Instant createdAt, boolean open) {
    public enum Kind { MID_COURSE, POST_COURSE }
}
