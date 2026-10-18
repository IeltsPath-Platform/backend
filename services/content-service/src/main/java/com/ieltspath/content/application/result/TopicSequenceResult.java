package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * A topic in the learning order, with the active knowledge points it measures. {@code requiredFeatureKey} is the
 * Access feature needed to learn it; null means free. {@code skill} is the one skill its lessons teach;
 * {@code hasTopicTest} is false when the topic has no published final test and is passed by completing its lessons.
 * {@code lessonCount} is the number of published lessons in the topic.
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
        List<Skill> skills,
        boolean active,
        int lessonCount
) {
    public TopicSequenceResult {
        skills = skills.stream().filter(s -> s != Skill.ALL).distinct().sorted().toList();
        skill = skills.size() == 1 ? skills.getFirst() : null;
    }

    public TopicSequenceResult(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                               List<KnowledgePointEntry> knowledgePoints, Skill skill, boolean hasTopicTest,
                               CourseEntry course) {
        this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course,
                skill == null ? List.of() : List.of(skill), true, 0);
    }

    public TopicSequenceResult(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                               List<KnowledgePointEntry> knowledgePoints, Skill skill, boolean hasTopicTest,
                               CourseEntry course, List<Skill> skills) {
        this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course, skills,
                true, 0);
    }

    public TopicSequenceResult(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                               List<KnowledgePointEntry> knowledgePoints, Skill skill, boolean hasTopicTest) {
        this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, null);
    }

    public record CourseEntry(UUID courseId, String code, String name, BigDecimal bandLevel, boolean hasCourseTest,
                              boolean active) {
        public CourseEntry(UUID courseId, String code, String name, BigDecimal bandLevel, boolean hasCourseTest) {
            this(courseId, code, name, bandLevel, hasCourseTest, true);
        }
    }

    /** {@code hasPracticeSet}: at least one practice set is eligible for review of this knowledge point. */
    public record KnowledgePointEntry(
            UUID id,
            UUID topicId,
            String code,
            String name,
            LearningType learningType,
            Skill skill,
            String description,
            boolean hasPracticeSet,
            CefrLevel cefrLevel,
            String canDo,
            String scopeIn,
            String scopeOut
    ) {
        public KnowledgePointEntry(UUID id, UUID topicId, String code, String name, LearningType learningType,
                                   Skill skill, String description, boolean hasPracticeSet) {
            this(id, topicId, code, name, learningType, skill, description, hasPracticeSet,
                    null, null, null, null);
        }
    }
}
