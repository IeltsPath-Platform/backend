package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.CourseResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResponse(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                            int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                            int completedLessonCount, int totalLessonCount, Instant lastStudiedAt,
                            String coverImageUrl, String description) {
    public CourseResponse(UUID courseId, String code, String name, BigDecimal bandLevel, int topicCount,
                          int passedTopicCount, boolean recommended, String testStatus, Instant passedAt,
                          int completedLessonCount, int totalLessonCount, Instant lastStudiedAt) {
        this(courseId, code, name, bandLevel, topicCount, passedTopicCount, recommended, testStatus, passedAt,
                completedLessonCount, totalLessonCount, lastStudiedAt, null, null);
    }

    public static CourseResponse from(CourseResult result) {
        return new CourseResponse(result.courseId(), result.code(), result.name(), result.bandLevel(),
                result.topicCount(), result.passedTopicCount(), result.recommended(), result.testStatus(),
                result.passedAt(), result.completedLessonCount(), result.totalLessonCount(), result.lastStudiedAt(),
                result.coverImageUrl(), result.description());
    }
}
