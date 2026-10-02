package com.group01.learning.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One topic in a learner's curriculum, changed only through {@code LearnerCurriculum}. {@code sequenceOrder} is the
 * Content order (null once Content removes the topic); {@code passedAt} is set once and never cleared.
 */
public final class TopicProgress {
    private final UUID topicId;
    private Integer sequenceOrder;
    private Instant passedAt;

    public TopicProgress(UUID topicId, Integer sequenceOrder, Instant passedAt) {
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.sequenceOrder = sequenceOrder;
        this.passedAt = passedAt;
    }

    public UUID topicId() { return topicId; }
    public Integer sequenceOrder() { return sequenceOrder; }
    public Instant passedAt() { return passedAt; }

    public void order(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    /** True when this call passed the topic; passing is one way. */
    public boolean pass(Instant now) {
        if (passedAt != null) return false;
        passedAt = Objects.requireNonNull(now, "now");
        return true;
    }
}
