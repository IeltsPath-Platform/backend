package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.vo.LearningSkill;

import java.util.List;
import java.util.UUID;

/** {@code accessLevel}: {@code FREE}, or {@code PREMIUM} when the topic needs a paid plan to be learned. */
public record TopicResponse(UUID topicId, String code, String name, int sequenceOrder,
                            String status, int completedLessonCount, String accessLevel,
                            LearningSkill skill, boolean hasTopicTest, TopicResult.Course course,
                            List<LearningSkill> skills) {
    public static TopicResponse from(TopicResult result) {
        return new TopicResponse(result.topicId(), result.code(), result.name(), result.sequenceOrder(),
                result.status().name(), result.completedLessonCount(),
                result.requiredFeatureKey() == null ? "FREE" : "PREMIUM", result.skill(), result.hasTopicTest(),
                result.course(), result.skills().stream().sorted().toList());
    }
}
