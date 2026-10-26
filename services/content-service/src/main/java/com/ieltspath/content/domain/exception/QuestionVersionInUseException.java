package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class QuestionVersionInUseException extends ContentDomainException {
    public QuestionVersionInUseException(UUID id) {
        super("Question version is used by published content: " + id);
    }
}
