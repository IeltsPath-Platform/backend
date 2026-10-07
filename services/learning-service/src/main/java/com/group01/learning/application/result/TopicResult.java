package com.group01.learning.application.result;

import com.group01.learning.domain.vo.TopicStatus;
import com.group01.learning.domain.vo.LearningSkill;
import java.util.UUID;

/** {@code requiredFeatureKey} is the Access feature Content requires to learn the topic; null means free. */
public record TopicResult(UUID topicId, String code, String name, int sequenceOrder,
                          TopicStatus status, int completedLessonCount, String requiredFeatureKey,
                          LearningSkill skill, boolean hasTopicTest, Course course, boolean hasCourseTest) {
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
