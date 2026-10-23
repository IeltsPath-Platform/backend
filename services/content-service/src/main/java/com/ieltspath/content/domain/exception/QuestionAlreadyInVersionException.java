package com.ieltspath.content.domain.exception;

import java.util.UUID;

/** A question version may appear once in a package version, in any section of it. */
public class QuestionAlreadyInVersionException extends ContentDomainException {
    public QuestionAlreadyInVersionException(UUID questionVersionId) {
        super("Question version is already in this package version: " + questionVersionId);
    }
}
