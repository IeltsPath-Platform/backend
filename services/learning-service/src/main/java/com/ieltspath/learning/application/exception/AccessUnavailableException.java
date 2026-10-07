package com.ieltspath.learning.application.exception;

/** Access Service could not be reached, timed out, or rejected the request (for example an expired token). */
public class AccessUnavailableException extends RuntimeException {
    public AccessUnavailableException(Integer status) {
        super("Access Service request failed (status " + status + ")");
    }
}