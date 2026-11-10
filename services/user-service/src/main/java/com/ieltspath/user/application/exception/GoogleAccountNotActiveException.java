package com.ieltspath.user.application.exception;

import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.vo.UserStatus;

import java.util.UUID;

public class GoogleAccountNotActiveException extends AuthenticationFailedException {
    private final UUID userId;
    private final UserStatus status;

    public GoogleAccountNotActiveException(UUID userId, UserStatus status) {
        super("Google sign-in is not available for this account");
        this.userId = userId;
        this.status = status;
    }

    public UUID userId() {
        return userId;
    }

    public UserStatus status() {
        return status;
    }
}
