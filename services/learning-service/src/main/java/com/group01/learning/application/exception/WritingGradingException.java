package com.group01.learning.application.exception;

/**
 * Grading produced no usable result: {@code INVALID_PROMPT} (the question cannot be graded), {@code LLM_UNAVAILABLE}
 * or {@code INVALID_GRADE} (the model's answer did not fit the schema, even after one retry).
 */
public class WritingGradingException extends RuntimeException {
    private final String code;

    public WritingGradingException(String code) {
        super("Essay grading failed: " + code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}