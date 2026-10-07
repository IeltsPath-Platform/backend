package com.group01.learning.application.result;

import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.TopicStatus;

import java.util.Set;
import java.util.UUID;

/** {@code requiredFeatureKey} is the Access feature Content requires to learn the topic; null means free. */
public record TopicResult(UUID topicId, String code, String name, int sequenceOrder,
                          TopicStatus status, int completedLessonCount, String requiredFeatureKey,
                          LearningSkill skill, boolean hasTopicTest, Course course, boolean hasCourseTest,
                          Set<LearningSkill> skills) {
    public TopicResult {
        skills = skills == null ? (skill == null ? Set.of() : Set.of(skill)) : skills;
    }
    public TopicResult(UUID topicId, String code, String name, int sequenceOrder, TopicStatus status,
                       int completedLessonCount, String requiredFeatureKey, LearningSkill skill,
                       boolean hasTopicTest, Course course, boolean hasCourseTest) {
        this(topicId, code, name, sequenceOrder, status, completedLessonCount, requiredFeatureKey, skill,
                hasTopicTest, course, hasCourseTest, null);
    }
    public TopicResult(UUID topicId, String code, String name, int sequenceOrder, TopicStatus status,
                       int completedLessonCount, String requiredFeatureKey, LearningSkill skill,
                       boolean hasTopicTest) {
        this(topicId, code, name, sequenceOrder, status, completedLessonCount, requiredFeatureKey, skill,
                hasTopicTest, null, false);
    }
    public TopicResult(UUID topicId, String code, String name, int sequenceOrder,
                       TopicStatus status, int completedLessonCount, String requiredFeatureKey) {
        this(topicId, code, name, sequenceOrder, status, completedLessonCount, requiredFeatureKey, null, true, null,
                false);
    }

    public record Course(UUID courseId, String code, String name, java.math.BigDecimal bandLevel) {}
}
