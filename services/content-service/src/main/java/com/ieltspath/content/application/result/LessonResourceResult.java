package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.vo.LessonResourceType;

import java.time.Instant;
import java.util.UUID;

public record LessonResourceResult(UUID id, UUID lessonId, LessonResourceType resourceType, String title, String url,
                                   int sortOrder, Instant createdAt, Instant updatedAt) {
    public static LessonResourceResult from(LessonResource resource) {
        return new LessonResourceResult(resource.getId(), resource.getLessonId(), resource.getResourceType(),
                resource.getTitle(), resource.getUrl(), resource.getSortOrder(), resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
