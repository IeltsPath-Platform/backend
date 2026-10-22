package com.ieltspath.content.domain.exception;

public class LessonSortConflictException extends ContentDomainException {
    public LessonSortConflictException() {
        super("The supplied ordering must contain each current item exactly once");
    }

    public LessonSortConflictException(String message) {
        super(message);
    }
}
