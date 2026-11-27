package com.ieltspath.community.domain.exception;

public class CommunityForbiddenException extends CommunityException {
    private final String code;

    public CommunityForbiddenException(String message) {
        this(message, null);
    }

    public CommunityForbiddenException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
