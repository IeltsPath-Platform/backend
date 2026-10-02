package com.group01.learning.api.dto.response;

import com.group01.learning.application.result.TopicResult;

import java.util.UUID;

/** {@code accessLevel}: {@code FREE}, or {@code PREMIUM} when the topic needs a paid plan to be learned. */
public record TopicResponse(UUID topicId, String code, String name, int sequenceOrder,
                            String status, int completedLessonCount, String accessLevel) {
    public static TopicResponse from(TopicResult result) {
        return new TopicResponse(result.topicId(), result.code(), result.name(), result.sequenceOrder(),
                result.status().name(), result.completedLessonCount(),
                result.requiredFeatureKey() == null ? "FREE" : "PREMIUM");
    }
}
