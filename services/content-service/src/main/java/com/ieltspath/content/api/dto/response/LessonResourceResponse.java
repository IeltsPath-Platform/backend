package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.domain.vo.LessonResourceType;

import java.time.Instant;
import java.util.UUID;

public record LessonResourceResponse(UUID id, UUID lessonId, LessonResourceType type, String title, String url,
                                     int sortOrder, Instant createdAt, Instant updatedAt) {
    public static LessonResourceResponse from(LessonResourceResult result) {
        return new LessonResourceResponse(result.id(), result.lessonId(), result.resourceType(), result.title(),
                result.url(), result.sortOrder(), result.createdAt(), result.updatedAt());
    }
}
