package com.ieltspath.content.domain.exception;

public class LessonExerciseLockedException extends ContentDomainException {
    public LessonExerciseLockedException() {
        super("Exercise blocks can only change while the lesson is a draft");
    }
}
