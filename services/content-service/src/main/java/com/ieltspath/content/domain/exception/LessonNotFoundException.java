package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class LessonNotFoundException extends ContentDomainException {
    public LessonNotFoundException(UUID id) {
        super("Lesson not found with id: " + id);
    }
}
