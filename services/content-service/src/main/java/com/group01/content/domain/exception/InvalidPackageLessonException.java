package com.group01.content.domain.exception;

/** A package cannot belong to that lesson: only a practice set may, and its questions must share the lesson's skill. */
public class InvalidPackageLessonException extends ContentDomainException {
    public InvalidPackageLessonException(String message) {
        super(message);
    }
}
