package com.group01.content.domain.exception;

import java.util.UUID;

public class QuestionNotFoundException extends ContentDomainException {
    public QuestionNotFoundException(UUID id) {
        super("Question not found with id: " + id);
    }
}

