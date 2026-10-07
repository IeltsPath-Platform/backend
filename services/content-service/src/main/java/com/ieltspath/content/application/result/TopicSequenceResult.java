package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;

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
        CourseEntry course,
        List<Skill> skills
) {
    public TopicSequenceResult {
        skills = skills.stream().filter(s -> s != Skill.ALL).distinct().sorted().toList();
        skill = skills.size() == 1 ? skills.getFirst() : null;
    }

    public TopicSequenceResult(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                               List<KnowledgePointEntry> knowledgePoints, Skill skill, boolean hasTopicTest,
                               CourseEntry course) {
        this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course,
                skill == null ? List.of() : List.of(skill));
    }

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
