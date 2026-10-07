package com.ieltspath.assessment.application.exception;

/** Content Service could not be reached or returned a response Assessment cannot use. */
public class ContentUnavailableException extends RuntimeException {
    public ContentUnavailableException(String message) {
        super(message);
    }

    public ContentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
