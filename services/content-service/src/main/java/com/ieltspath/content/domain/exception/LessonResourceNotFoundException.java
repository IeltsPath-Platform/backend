package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class LessonResourceNotFoundException extends ContentDomainException {
    public LessonResourceNotFoundException(UUID lessonId, UUID resourceId) {
        super("Lesson resource '" + resourceId + "' not found in lesson '" + lessonId + "'");
    }
}
