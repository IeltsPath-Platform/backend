package com.ieltspath.content.domain.exception;

public class QuestionVersionNotPublishableException extends ContentDomainException {
    public QuestionVersionNotPublishableException() {
        super("Question version does not satisfy publication requirements");
    }
}
