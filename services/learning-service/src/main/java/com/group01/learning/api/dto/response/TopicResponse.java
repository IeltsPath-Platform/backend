package com.group01.learning.api.dto;

import com.group01.learning.application.result.TopicResult;
import java.util.UUID;

public record TopicResponse(UUID topicId, String code, String name, int sequenceOrder,
                            String status, int completedLessonCount) {
    public static TopicResponse from(TopicResult result) {
        return new TopicResponse(result.topicId(), result.code(), result.name(), result.sequenceOrder(),
                result.status().name(), result.completedLessonCount());
    }
}
