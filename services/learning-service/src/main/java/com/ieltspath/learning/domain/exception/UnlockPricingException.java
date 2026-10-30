package com.ieltspath.learning.domain.exception;

public class UnlockPricingException extends RuntimeException {
    private final Code code;

    public UnlockPricingException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code code() { return code; }

    public enum Code { NOT_FOR_SALE, SKILL_NOT_AVAILABLE }
}
