package com.ieltspath.content.domain.exception;

public class DifficultyRequiredException extends ContentDomainException {
    public DifficultyRequiredException() {
        super("Question difficulty is required");
    }
}
