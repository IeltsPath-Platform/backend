package com.group01.content.domain.exception;

import java.util.UUID;

public class LessonNotFoundException extends ContentDomainException {
    public LessonNotFoundException(UUID id) {
        super("Lesson not found with id: " + id);
    }
}
