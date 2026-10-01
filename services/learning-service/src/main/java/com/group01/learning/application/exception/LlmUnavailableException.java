package com.group01.learning.application.exception;

/** The LLM provider failed or timed out. Carries the HTTP status only, never provider text. */
public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(Integer status) {
        super("LLM provider request failed (status " + status + ")");
    }
}