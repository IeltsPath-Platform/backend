package com.group01.content.api.dto.internal;

import com.group01.content.application.result.TopicSequenceResult;
import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.Skill;

import java.util.List;
import java.util.UUID;

public record TopicSequenceResponse(
        UUID topicId,
        String code,
        String name,
        int sortOrder,
        String requiredFeatureKey,
        List<KnowledgePoint> knowledgePoints,
        Skill skill,
        boolean hasTopicTest,
        Course course
) {
    public record Course(UUID courseId, String code, String name, java.math.BigDecimal bandLevel, boolean hasCourseTest) {
        static Course from(TopicSequenceResult.CourseEntry entry) {
            return entry == null ? null : new Course(entry.courseId(), entry.code(), entry.name(), entry.bandLevel(), entry.hasCourseTest());
        }
    }
    public record KnowledgePoint(UUID id, String code, String name, LearningType learningType, Skill skill,
                                 String description, boolean hasPracticeSet) {}

    public static TopicSequenceResponse from(TopicSequenceResult result) {
        return new TopicSequenceResponse(result.topicId(), result.code(), result.name(), result.sortOrder(),
                result.requiredFeatureKey(),
                result.knowledgePoints().stream()
                        .map(kp -> new KnowledgePoint(kp.id(), kp.code(), kp.name(), kp.learningType(), kp.skill(),
                                kp.description(), kp.hasPracticeSet()))
                        .toList(),
                result.skill(), result.hasTopicTest(), Course.from(result.course()));
    }
}
