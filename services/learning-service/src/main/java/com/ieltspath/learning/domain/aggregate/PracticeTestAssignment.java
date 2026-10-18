package com.ieltspath.learning.domain.aggregate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** The first completed result of an assigned package is immutable, including after a regrade or retake. */
public final class PracticeTestAssignment {
    private final UUID id, userId, courseId, packageId, packageVersionId;
    private final Instant assignedAt;
    private UUID consumedAttemptId;
    private Instant consumedAt;
    private BigDecimal percent;

    private PracticeTestAssignment(UUID id, UUID userId, UUID courseId, UUID packageId, UUID packageVersionId,
                                   Instant assignedAt, UUID attemptId, Instant consumedAt, BigDecimal percent) {
        this.id=Objects.requireNonNull(id); this.userId=Objects.requireNonNull(userId);
        this.courseId=Objects.requireNonNull(courseId); this.packageId=Objects.requireNonNull(packageId);
        this.packageVersionId=Objects.requireNonNull(packageVersionId); this.assignedAt=Objects.requireNonNull(assignedAt);
        this.consumedAttemptId=attemptId; this.consumedAt=consumedAt; this.percent=percent;
    }
    public static PracticeTestAssignment assign(UUID userId, UUID courseId, UUID packageId, UUID versionId, Instant now) {
        return new PracticeTestAssignment(UUID.randomUUID(),userId,courseId,packageId,versionId,now,null,null,null);
    }
    public static PracticeTestAssignment restore(UUID id, UUID userId, UUID courseId, UUID packageId, UUID versionId,
                                                 Instant assignedAt, UUID attemptId, Instant consumedAt, BigDecimal percent) {
        return new PracticeTestAssignment(id,userId,courseId,packageId,versionId,assignedAt,attemptId,consumedAt,percent);
    }
    public boolean consume(UUID attemptId, Instant completedAt, BigDecimal percent) {
        Objects.requireNonNull(attemptId); Objects.requireNonNull(completedAt); Objects.requireNonNull(percent);
        if (isConsumed() || completedAt.isBefore(assignedAt)) return false;
        if (percent.signum()<0 || percent.compareTo(BigDecimal.valueOf(100))>0) throw new IllegalArgumentException("Invalid percentage");
        this.consumedAttemptId=attemptId; this.consumedAt=completedAt; this.percent=percent;
        return true;
    }
    public boolean isConsumed() { return consumedAttemptId!=null; }
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID courseId() { return courseId; }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public Instant assignedAt() { return assignedAt; }
    public UUID consumedAttemptId() { return consumedAttemptId; }
    public Instant consumedAt() { return consumedAt; }
    public BigDecimal percent() { return percent; }
}
