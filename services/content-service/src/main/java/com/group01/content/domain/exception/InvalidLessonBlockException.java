package com.group01.content.domain.exception;

/** Seed data broke a lesson block rule; a data defect, not a client error. */
public class InvalidLessonBlockException extends ContentDomainException {
    public InvalidLessonBlockException(String message) {
        super(message);
    }
}
