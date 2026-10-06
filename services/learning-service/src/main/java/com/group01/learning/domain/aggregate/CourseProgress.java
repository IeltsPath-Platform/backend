package com.group01.learning.domain.aggregate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One-way completion of a course after passing its course test. */
public final class CourseProgress {
    private final UUID userId;
    private final UUID courseId;
    private Instant passedAt;

    private CourseProgress(UUID userId, UUID courseId, Instant passedAt) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.passedAt = passedAt;
    }

    public static CourseProgress restore(UUID userId, UUID courseId, Instant passedAt) {
        return new CourseProgress(userId, courseId, passedAt);
    }

    public boolean pass(Instant now) {
        if (passedAt != null) return false;
        passedAt = Objects.requireNonNull(now, "now");
        return true;
    }

    public UUID userId() { return userId; }
    public UUID courseId() { return courseId; }
    public Instant passedAt() { return passedAt; }
}
