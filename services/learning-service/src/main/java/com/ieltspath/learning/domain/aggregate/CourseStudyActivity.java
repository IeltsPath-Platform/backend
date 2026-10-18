package com.ieltspath.learning.domain.aggregate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** First and latest authorized study action; legacy first-study dates remain unknown. */
public record CourseStudyActivity(UUID userId, UUID courseId, Instant startedAt, Instant lastStudiedAt) {
    public CourseStudyActivity {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(courseId);
        Objects.requireNonNull(lastStudiedAt);
        if (startedAt != null && lastStudiedAt.isBefore(startedAt))
            throw new IllegalArgumentException("Last study cannot precede first study");
    }

    public static CourseStudyActivity start(UUID userId, UUID courseId, Instant now, boolean legacy) {
        return new CourseStudyActivity(userId, courseId, legacy ? null : now, now);
    }

    public CourseStudyActivity studiedAt(Instant now) {
        return new CourseStudyActivity(userId, courseId, startedAt,
                now.isAfter(lastStudiedAt) ? now : lastStudiedAt);
    }
}
