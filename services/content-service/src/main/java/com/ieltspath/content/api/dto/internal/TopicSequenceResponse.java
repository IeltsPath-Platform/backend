package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        int lessonCount,
        String description
) {
    private static final Logger log = LoggerFactory.getLogger(TopicSequenceResponse.class);
    public TopicSequenceResponse(UUID topicId, String code, String name, int sortOrder, String requiredFeatureKey,
                                 List<KnowledgePoint> knowledgePoints, Skill skill, boolean hasTopicTest, Course course,
                                 List<Skill> skills, boolean active, int lessonCount) {
        this(topicId, code, name, sortOrder, requiredFeatureKey, knowledgePoints, skill, hasTopicTest, course, skills,
                active, lessonCount, null);
    }

    public record Course(UUID courseId, String code, String name, java.math.BigDecimal bandLevel,
                         boolean hasCourseTest, boolean active, String coverImageUrl, String description,
                         java.util.Map<Skill, Integer> skillUnlockPoints, Integer comboUnlockPoints,
                         List<Skill> availableSkills) {
        public Course {
            skillUnlockPoints = skillUnlockPoints == null ? java.util.Map.of() : java.util.Map.copyOf(skillUnlockPoints);
            availableSkills = availableSkills == null ? List.of() : List.copyOf(availableSkills);
        }
        public Course(UUID courseId, String code, String name, java.math.BigDecimal bandLevel,
                      boolean hasCourseTest, boolean active) {
            this(courseId, code, name, bandLevel, hasCourseTest, active, null, null, java.util.Map.of(), null, List.of());
        }
        public Course(UUID courseId, String code, String name, java.math.BigDecimal bandLevel,
                      boolean hasCourseTest, boolean active, String coverImageUrl, String description) {
            this(courseId, code, name, bandLevel, hasCourseTest, active, coverImageUrl, description,
                    java.util.Map.of(), null, List.of());
        }

        static Course from(TopicSequenceResult.CourseEntry entry, MediaReferencePolicy mediaPolicy) {
            return entry == null ? null : new Course(entry.courseId(), entry.code(), entry.name(), entry.bandLevel(),
                    entry.hasCourseTest(), entry.active(), resolveCoverImage(entry.coverImageUrl(), mediaPolicy), entry.description(),
                    entry.skillUnlockPoints(), entry.comboUnlockPoints(), entry.availableSkills());
        }

        static Course from(TopicSequenceResult.CourseEntry entry) {
            return from(entry, null);
        }
    }
    public record KnowledgePoint(UUID id, String code, String name, LearningType learningType, Skill skill,
                                 String description, boolean hasPracticeSet, CefrLevel cefrLevel, String canDo,
                                 String scopeIn, String scopeOut, UUID parentKnowledgePointId, String errorCode,
                                 Integer earliestLessonPosition) {
        public KnowledgePoint(UUID id, String code, String name, LearningType learningType, Skill skill,
                              String description, boolean hasPracticeSet, CefrLevel cefrLevel, String canDo,
                              String scopeIn, String scopeOut, UUID parentKnowledgePointId, String errorCode) {
            this(id, code, name, learningType, skill, description, hasPracticeSet, cefrLevel, canDo, scopeIn,
                    scopeOut, parentKnowledgePointId, errorCode, null);
        }
    }

    public static TopicSequenceResponse from(TopicSequenceResult result) {
        return from(result, null);
    }

    public static TopicSequenceResponse from(TopicSequenceResult result, MediaReferencePolicy mediaPolicy) {
        return new TopicSequenceResponse(result.topicId(), result.code(), result.name(), result.sortOrder(),
                result.requiredFeatureKey(),
                result.knowledgePoints().stream()
                        .map(kp -> new KnowledgePoint(kp.id(), kp.code(), kp.name(), kp.learningType(), kp.skill(),
                                kp.description(), kp.hasPracticeSet(), kp.cefrLevel(), kp.canDo(), kp.scopeIn(),
                                kp.scopeOut(), kp.parentKnowledgePointId(), kp.errorCode(), kp.earliestLessonPosition()))
                        .toList(),
                result.skill(), result.hasTopicTest(), Course.from(result.course(), mediaPolicy), result.skills(), result.active(),
                result.lessonCount(), result.description());
    }

    private static String resolveCoverImage(String reference, MediaReferencePolicy mediaPolicy) {
        if (reference == null || !reference.startsWith("content-image/") || mediaPolicy == null) return reference;
        try {
            return mediaPolicy.resolve(AssetType.IMAGE, reference);
        } catch (RuntimeException unresolvable) {
            log.warn("Course cover image key could not be resolved in topic sequence");
            return null;
        }
    }
}
