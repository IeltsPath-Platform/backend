package com.ieltspath.content.domain.exception;

public class DuplicateCourseException extends ContentDomainException {
    public DuplicateCourseException(String field, String value) {
        super("Course with " + field + " '" + value + "' already exists");
    }
}
