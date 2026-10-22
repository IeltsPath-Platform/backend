package com.ieltspath.user.domain.exception;

public class UserManagementRuleViolationException extends RuntimeException {
    private final String code;

    public UserManagementRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
