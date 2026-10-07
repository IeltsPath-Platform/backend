package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.vo.ContentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResult(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                           Instant createdAt, Instant updatedAt) {
    public static CourseResult from(Course course) {
        return new CourseResult(course.getId(), course.getCode(), course.getName(), course.getBandLevel(),
                course.getStatus(), course.getCreatedAt(), course.getUpdatedAt());
    }
}
