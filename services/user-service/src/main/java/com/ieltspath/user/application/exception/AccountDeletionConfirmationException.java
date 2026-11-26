package com.ieltspath.user.application.exception;

public class AccountDeletionConfirmationException extends RuntimeException {
    private final String code;

    public AccountDeletionConfirmationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
