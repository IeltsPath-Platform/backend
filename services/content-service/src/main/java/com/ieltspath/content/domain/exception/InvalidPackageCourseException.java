package com.ieltspath.content.domain.exception;

/** A course final test needs an existing course and nothing that belongs to another kind of package. */
public class InvalidPackageCourseException extends ContentDomainException {
    public InvalidPackageCourseException(String message) {
        super(message);
    }
}
