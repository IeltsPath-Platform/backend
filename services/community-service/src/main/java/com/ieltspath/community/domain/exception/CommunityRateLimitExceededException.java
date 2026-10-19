package com.ieltspath.community.domain.exception;

public class CommunityRateLimitExceededException extends CommunityException {

    public CommunityRateLimitExceededException(String message) {
        super(message);
    }
}

