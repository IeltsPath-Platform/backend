package com.ieltspath.assessment.application.exception;

/** The user service could not be reached, so an examiner cannot be checked. */
public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(String message) { super(message); }

    public UserServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
}
