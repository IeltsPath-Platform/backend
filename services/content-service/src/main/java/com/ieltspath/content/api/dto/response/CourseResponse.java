package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.domain.vo.ContentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseResponse(UUID id, String code, String name, BigDecimal bandLevel, ContentStatus status,
                             Instant createdAt, Instant updatedAt) {
    public static CourseResponse from(CourseResult result) {
        return new CourseResponse(result.id(), result.code(), result.name(), result.bandLevel(), result.status(),
                result.createdAt(), result.updatedAt());
    }
}
