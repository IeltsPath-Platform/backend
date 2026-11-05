package com.ieltspath.content.application.result;

import com.ieltspath.content.application.port.LessonDocumentFiles;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.vo.LessonResourceType;

import java.time.Instant;
import java.util.UUID;

/** {@code url} is what a browser may open: the stored link, or a short-lived signed link for an uploaded file. */
public record LessonResourceResult(UUID id, UUID lessonId, LessonResourceType resourceType, String title, String url,
                                   String fileKey, int sortOrder, Instant createdAt, Instant updatedAt) {
    /** For a link resource: an uploaded file comes back without a url, so read those with the two-argument form. */
    public static LessonResourceResult from(LessonResource resource) {
        return new LessonResourceResult(resource.getId(), resource.getLessonId(), resource.getResourceType(),
                resource.getTitle(), resource.getUrl(), resource.getFileKey(), resource.getSortOrder(),
                resource.getCreatedAt(), resource.getUpdatedAt());
    }

    public static LessonResourceResult from(LessonResource resource, LessonDocumentFiles files) {
        String url = resource.getFileKey() == null ? resource.getUrl() : files.signedUrl(resource.getFileKey());
        return new LessonResourceResult(resource.getId(), resource.getLessonId(), resource.getResourceType(),
                resource.getTitle(), url, resource.getFileKey(), resource.getSortOrder(), resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
