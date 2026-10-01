package com.group01.library.application.exception;

public class TopicServiceUnavailableException extends RuntimeException {
    public TopicServiceUnavailableException(Throwable cause) {
        super("Topic service is unavailable", cause);
    }
}
