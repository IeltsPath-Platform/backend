package com.group01.content.domain.exception;

import java.util.UUID;

public class CourseNotFoundException extends ContentDomainException {
    public CourseNotFoundException(UUID id) {
        super("Course with id '" + id + "' not found");
    }
}
