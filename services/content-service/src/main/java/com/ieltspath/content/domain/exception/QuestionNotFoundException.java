package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class QuestionNotFoundException extends ContentDomainException {
    public QuestionNotFoundException(UUID id) {
        super("Question not found with id: " + id);
    }
}

