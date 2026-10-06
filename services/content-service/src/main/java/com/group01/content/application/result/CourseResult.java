package com.group01.content.application.result;

import com.group01.content.domain.aggregate.Course;
import com.group01.content.domain.vo.ContentStatus;
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
