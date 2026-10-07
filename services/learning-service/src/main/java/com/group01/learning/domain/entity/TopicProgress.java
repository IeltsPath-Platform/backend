package com.group01.learning.domain.entity;

import com.group01.learning.domain.vo.LearningSkill;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
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
    private Set<LearningSkill> skills;
    private boolean hasTopicTest;

    public TopicProgress(UUID topicId, Integer sequenceOrder, Instant passedAt) {
        this(topicId, null, sequenceOrder, passedAt, (LearningSkill) null, true);
    }

    public TopicProgress(UUID topicId, Integer sequenceOrder, Instant passedAt,
                         LearningSkill skill, boolean hasTopicTest) {
        this(topicId, null, sequenceOrder, passedAt, skill, hasTopicTest);
    }

    public TopicProgress(UUID topicId, UUID courseId, Integer sequenceOrder, Instant passedAt,
                         LearningSkill skill, boolean hasTopicTest) {
        this(topicId, courseId, sequenceOrder, passedAt, single(skill), hasTopicTest);
    }

    private TopicProgress(UUID topicId, UUID courseId, Integer sequenceOrder, Instant passedAt,
                          Set<LearningSkill> skills, boolean hasTopicTest) {
        this.topicId = Objects.requireNonNull(topicId, "topicId");
        this.courseId = courseId;
        this.sequenceOrder = sequenceOrder;
        this.passedAt = passedAt;
        this.skills = copy(skills);
        this.hasTopicTest = hasTopicTest;
    }

    /** A topic whose lessons teach {@code skills}; a topic of several skills has no single {@link #skill()}. */
    public static TopicProgress withSkills(UUID topicId, UUID courseId, Integer sequenceOrder, Instant passedAt,
                                           Set<LearningSkill> skills, boolean hasTopicTest) {
        return new TopicProgress(topicId, courseId, sequenceOrder, passedAt, skills, hasTopicTest);
    }

    public UUID topicId() { return topicId; }
    public UUID courseId() { return courseId; }
    public Integer sequenceOrder() { return sequenceOrder; }
    public Instant passedAt() { return passedAt; }
    /** The one skill of the topic, or null when it teaches none or several. */
    public LearningSkill skill() { return skills.size() == 1 ? skills.iterator().next() : null; }
    public Set<LearningSkill> skills() { return skills; }
    public boolean hasTopicTest() { return hasTopicTest; }

    public void order(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    public void place(Integer sequenceOrder, LearningSkill skill, boolean hasTopicTest) {
        place(sequenceOrder, courseId, skill, hasTopicTest);
    }
    public void place(Integer sequenceOrder, UUID courseId, LearningSkill skill, boolean hasTopicTest) {
        place(sequenceOrder, courseId, single(skill), hasTopicTest);
    }
    public void place(Integer sequenceOrder, UUID courseId, Set<LearningSkill> skills, boolean hasTopicTest) {
        this.sequenceOrder = sequenceOrder;
        this.courseId = courseId;
        this.skills = copy(skills);
        this.hasTopicTest = hasTopicTest;
    }

    private static Set<LearningSkill> single(LearningSkill skill) {
        return skill == null ? Set.of() : Set.of(skill);
    }

    private static Set<LearningSkill> copy(Set<LearningSkill> skills) {
        EnumSet<LearningSkill> copy = EnumSet.noneOf(LearningSkill.class);
        if (skills != null) copy.addAll(skills);
        return Collections.unmodifiableSet(copy);
    }

    /** True when this call passed the topic; passing is one way. */
    public boolean pass(Instant now) {
        if (passedAt != null) return false;
        passedAt = Objects.requireNonNull(now, "now");
        return true;
    }
}
