package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.Skill;

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
        Course course,
        List<Skill> skills,
        boolean active,
        int lessonCount
) {
    public record Course(UUID courseId, String code, String name, java.math.BigDecimal bandLevel,
                         boolean hasCourseTest, boolean active) {
        static Course from(TopicSequenceResult.CourseEntry entry) {
            return entry == null ? null : new Course(entry.courseId(), entry.code(), entry.name(), entry.bandLevel(),
                    entry.hasCourseTest(), entry.active());
        }
    }
    public record KnowledgePoint(UUID id, String code, String name, LearningType learningType, Skill skill,
                                 String description, boolean hasPracticeSet, CefrLevel cefrLevel, String canDo,
                                 String scopeIn, String scopeOut) {}

    public static TopicSequenceResponse from(TopicSequenceResult result) {
        return new TopicSequenceResponse(result.topicId(), result.code(), result.name(), result.sortOrder(),
                result.requiredFeatureKey(),
                result.knowledgePoints().stream()
                        .map(kp -> new KnowledgePoint(kp.id(), kp.code(), kp.name(), kp.learningType(), kp.skill(),
                                kp.description(), kp.hasPracticeSet(), kp.cefrLevel(), kp.canDo(), kp.scopeIn(),
                                kp.scopeOut()))
                        .toList(),
                result.skill(), result.hasTopicTest(), Course.from(result.course()), result.skills(), result.active(),
                result.lessonCount());
    }
}
