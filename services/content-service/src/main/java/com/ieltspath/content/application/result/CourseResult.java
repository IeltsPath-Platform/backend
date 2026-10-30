package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.vo.ContentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResult(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                           Instant createdAt, Instant updatedAt, String coverImageUrl, String description) {
    public CourseResult(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                        Instant createdAt, Instant updatedAt) {
        this(id, code, name, bandLevel, status, createdAt, updatedAt, null, null);
    }

    public static CourseResult from(Course course) {
        return new CourseResult(course.getId(), course.getCode(), course.getName(), course.getBandLevel(),
                course.getStatus(), course.getCreatedAt(), course.getUpdatedAt(), course.getCoverImageUrl(),
                course.getDescription());
    }
}
