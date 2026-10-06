package com.group01.learning.domain.entity;

import com.group01.learning.domain.vo.LearningSkill;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One topic in a learner's curriculum, changed only through {@code LearnerCurriculum}. {@code sequenceOrder} is the
 * Content order (null once Content removes the topic); {@code passedAt} is set once and never cleared.
 */
public final class TopicProgress {
    private final UUID topicId;
    private UUID courseId;
    private Integer sequenceOrder;
    private Instant passedAt;
    private LearningSkill skill;
    private boolean hasTopicTest;

    public TopicProgress(UUID topicId, Integer sequenceOrder, Instant passedAt) {
        this(topicId, null, sequenceOrder, passedAt, null, true);
    }

    public TopicProgress(UUID topicId, Integer sequenceOrder, Instant passedAt,
                         LearningSkill skill, boolean hasTopicTest) {
        this(topicId, null, sequenceOrder, passedAt, skill, hasTopicTest);
    }

    public TopicProgress(UUID topicId, UUID courseId, Integer sequenceOrder, Instant passedAt,
                         LearningSkill skill, boolean hasTopicTest) {
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.courseId = courseId;
        this.sequenceOrder = sequenceOrder;
        this.passedAt = passedAt;
        this.skill = skill;
        this.hasTopicTest = hasTopicTest;
    }

    public UUID topicId() { return topicId; }
    public UUID courseId() { return courseId; }
    public Integer sequenceOrder() { return sequenceOrder; }
    public Instant passedAt() { return passedAt; }
    public LearningSkill skill() { return skill; }
    public boolean hasTopicTest() { return hasTopicTest; }

    public void order(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public void place(Integer sequenceOrder, LearningSkill skill, boolean hasTopicTest) {
        place(sequenceOrder, courseId, skill, hasTopicTest);
    }
    public void place(Integer sequenceOrder, UUID courseId, LearningSkill skill, boolean hasTopicTest) {
        this.sequenceOrder = sequenceOrder;
        this.courseId = courseId;
        this.skill = skill;
        this.hasTopicTest = hasTopicTest;
    }

    /** True when this call passed the topic; passing is one way. */
    public boolean pass(Instant now) {
        if (passedAt != null) return false;
        passedAt = Objects.requireNonNull(now, "now");
        return true;
    }
}
