package com.ieltspath.learning.application.exception;

/**
 * The speech service could not analyse a recording. {@code retryable} is true for a failure that may pass (timeout,
 * overload, a service that is down) and false for one that will not (an unreadable file, a refused key).
 */
public class SpeechUnavailableException extends RuntimeException {
    private final boolean retryable;

    public SpeechUnavailableException(String code, boolean retryable) {
        super(code);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
