package com.ieltspath.user.domain.vo;

public enum LoginFailureReason {
    BAD_PASSWORD,
    ACCOUNT_LOCKED,
    ACCOUNT_INACTIVE,
    ACCOUNT_UNVERIFIED;

    /** Why an account that is not ACTIVE cannot sign in. */
    public static LoginFailureReason forInactiveStatus(UserStatus status) {
        return switch (status) {
            case LOCKED -> ACCOUNT_LOCKED;
            case PENDING_VERIFY -> ACCOUNT_UNVERIFIED;
            default -> ACCOUNT_INACTIVE;
        };
    }
}
