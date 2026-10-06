package com.group01.learning.api.dto.response;

import com.group01.learning.application.result.CourseResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResponse(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                            int passedTopicCount, boolean recommended, String testStatus, Instant passedAt) {
    public static CourseResponse from(CourseResult result) {
        return new CourseResponse(result.courseId(), result.code(), result.name(), result.bandLevel(),
                result.topicCount(), result.passedTopicCount(), result.recommended(), result.testStatus(),
                result.passedAt());
    }
}
