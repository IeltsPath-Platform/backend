package com.ieltspath.content.domain.exception;

import java.util.UUID;

public class SectionQuestionNotFoundException extends ContentDomainException {
    public SectionQuestionNotFoundException(UUID id) {
        super("Section question not found: " + id);
    }
}
