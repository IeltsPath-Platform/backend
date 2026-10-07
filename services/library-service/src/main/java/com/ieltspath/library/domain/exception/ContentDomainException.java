package com.ieltspath.library.domain.exception;

public class ContentDomainException extends RuntimeException {
    public ContentDomainException(String message) {
        super(message);
    }

    public ContentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
