package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.LessonResourceType;

import java.util.UUID;

/**
 * Exactly one of {@code url} and {@code fileKey} is given. A file key must belong to {@code uploaderId}, the author
 * who uploaded it.
 */
public record CreateLessonResourceCommand(UUID lessonId, LessonResourceType resourceType, String title, String url,
                                          String fileKey, UUID uploaderId, int sortOrder) {
    /** A link resource. */
    public CreateLessonResourceCommand(UUID lessonId, LessonResourceType resourceType, String title, String url,
                                       int sortOrder) {
        this(lessonId, resourceType, title, url, null, null, sortOrder);
    }
}
