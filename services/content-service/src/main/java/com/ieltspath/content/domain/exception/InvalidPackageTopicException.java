package com.ieltspath.content.domain.exception;

/** A topic final test needs an existing topic and nothing that belongs to another kind of package. */
public class InvalidPackageTopicException extends ContentDomainException {
    public InvalidPackageTopicException(String message) {
        super(message);
    }
}
