package com.ieltspath.content.domain.exception;

public class QuestionDifficultyRequiredForPublicationException extends ContentDomainException {
    public QuestionDifficultyRequiredForPublicationException() {
        super("Question difficulty is required before publication");
    }
}
