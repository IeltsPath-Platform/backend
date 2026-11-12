package com.ieltspath.user.application.exception;

public class GoogleEmailNotSupportedException extends RuntimeException {
    public GoogleEmailNotSupportedException() {
        super("This Google email is not supported");
    }
}
