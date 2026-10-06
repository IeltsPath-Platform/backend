package com.group01.content.application.result;

import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * A topic in the learning order, with the active knowledge points it measures. {@code requiredFeatureKey} is the
 * Access feature needed to learn it; null means free. {@code skill} is the one skill its lessons teach;
 * {@code hasTopicTest} is false when the topic has no published final test and is passed by completing its lessons.
 */
public record TopicSequenceResult(
        UUID topicId,
        String code,
        String name,
        int sortOrder,
        String requiredFeatureKey,
        List<KnowledgePointEntry> knowledgePoints,
        Skill skill,
        boolean hasTopicTest,
        CourseEntry course
) {
    public TopicSequenceResult(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                               List<KnowledgePointEntry> knowledgePoints, Skill skill, boolean hasTopicTest) {
        this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, null);
    }

    public record CourseEntry(UUID courseId, String code, String name, BigDecimal bandLevel, boolean hasCourseTest) {}

    /** {@code hasPracticeSet}: at least one practice set is eligible for review of this knowledge point. */
    public record KnowledgePointEntry(
            UUID id,
            UUID topicId,
            String code,
            String name,
            LearningType learningType,
            Skill skill,
            String description,
            boolean hasPracticeSet
    ) {}
}
