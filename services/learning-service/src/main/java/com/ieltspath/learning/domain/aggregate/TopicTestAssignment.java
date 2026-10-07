package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.service.PassMark;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A final-test code given to a learner for a topic. The first completed attempt of that package version after the
 * assignment consumes it, once; 70% or more passes the topic.
 */
public final class TopicTestAssignment {
    private final UUID id;
    private final UUID userId;
    private final UUID topicId;
    private final UUID packageId;
    private final UUID packageVersionId;
    private UUID consumedAttemptId;
    private BigDecimal percent;

    private TopicTestAssignment(UUID id, UUID userId, UUID topicId, UUID packageId, UUID packageVersionId,
                                UUID consumedAttemptId, BigDecimal percent) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.packageId = Objects.requireNonNull(packageId, "packageId");
        this.packageVersionId = Objects.requireNonNull(packageVersionId, "packageVersionId");
        this.consumedAttemptId = consumedAttemptId;
        this.percent = percent;
    }

    public static TopicTestAssignment assign(UUID id, UUID userId, UUID topicId, UUID packageId,
                                             UUID packageVersionId) {
        return new TopicTestAssignment(id, userId, topicId, packageId, packageVersionId, null, null);
    }

    public static TopicTestAssignment restore(UUID id, UUID userId, UUID topicId, UUID packageId,
                                              UUID packageVersionId, UUID consumedAttemptId, BigDecimal percent) {
        return new TopicTestAssignment(id, userId, topicId, packageId, packageVersionId, consumedAttemptId, percent);
    }

    /** Consumes the assignment with the attempt's score; returns whether the score passes the topic. */
    public boolean consume(UUID attemptId, BigDecimal percent) {
        if (isConsumed()) throw new IllegalStateException("Assignment is already consumed");
        this.consumedAttemptId = Objects.requireNonNull(attemptId, "attemptId");
        this.percent = Objects.requireNonNull(percent, "percent");
        return PassMark.passesPercent(percent);
    }

    public boolean isConsumed() { return consumedAttemptId != null; }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID topicId() { return topicId; }
    public UUID packageId() { return packageId; }
    public UUID packageVersionId() { return packageVersionId; }
    public UUID consumedAttemptId() { return consumedAttemptId; }
    public BigDecimal percent() { return percent; }
}
