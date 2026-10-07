package com.ieltspath.assessment.application.exception;

/** The LLM could not grade an essay; {@code code} names why and is safe to log. */
public class EssayGradingException extends RuntimeException {
    private final String code;

    public EssayGradingException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
