package com.group01.content.domain.exception;

public class ContentDomainException extends RuntimeException {
    public ContentDomainException(String message) {
        super(message);
    }

    public ContentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

