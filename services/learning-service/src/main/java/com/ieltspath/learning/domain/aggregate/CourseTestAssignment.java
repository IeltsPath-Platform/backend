package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.service.PassMark;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** A one-use final-test package assigned to a learner for a course. */
public final class CourseTestAssignment {
    private final UUID id;
    private final UUID userId;
    private final UUID courseId;
    private final UUID packageId;
    private final UUID packageVersionId;
    private UUID consumedAttemptId;
    private BigDecimal percent;

    private CourseTestAssignment(UUID id, UUID userId, UUID courseId, UUID packageId, UUID packageVersionId,
                                 UUID consumedAttemptId, BigDecimal percent) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.courseId = Objects.requireNonNull(courseId, "courseId");
        this.packageId = Objects.requireNonNull(packageId, "packageId");
        this.packageVersionId = Objects.requireNonNull(packageVersionId, "packageVersionId");
        this.consumedAttemptId = consumedAttemptId;
        this.percent = percent;
    }

    public static CourseTestAssignment assign(UUID id, UUID userId, UUID courseId, UUID packageId,
                                              UUID packageVersionId) {
        return new CourseTestAssignment(id, userId, courseId, packageId, packageVersionId, null, null);
    }

    public static CourseTestAssignment restore(UUID id, UUID userId, UUID courseId, UUID packageId,
                                               UUID packageVersionId, UUID consumedAttemptId, BigDecimal percent) {
        return new CourseTestAssignment(id, userId, courseId, packageId, packageVersionId, consumedAttemptId, percent);
    }

    public boolean consume(UUID attemptId, BigDecimal percent) {
        if (isConsumed()) throw new IllegalStateException("Assignment is already consumed");
        this.consumedAttemptId = Objects.requireNonNull(attemptId, "attemptId");
        this.percent = Objects.requireNonNull(percent, "percent");
        return PassMark.passesPercent(percent);
    }

    public boolean isConsumed() { return consumedAttemptId != null; }
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID courseId() { return courseId; }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public UUID consumedAttemptId() { return consumedAttemptId; }
    public BigDecimal percent() { return percent; }
}
