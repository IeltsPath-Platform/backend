package com.ieltspath.content.domain.exception;

/** An error type or a distractor mapping breaks a rule of the error taxonomy. */
public class InvalidErrorTypeException extends ContentDomainException {
    public InvalidErrorTypeException(String message) {
        super(message);
    }
}
