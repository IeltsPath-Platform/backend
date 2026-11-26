package com.ieltspath.events.user;

public final class UserMessaging {
    public static final String EXCHANGE = "user.events";
    public static final String PASSWORD_RESET_REQUESTED_ROUTING_KEY = "user.password-reset-requested.v1";
    public static final String PASSWORD_CHANGED_ROUTING_KEY = "user.password-changed.v1";
    public static final String GOOGLE_ACCOUNT_LINKED_ROUTING_KEY = "user.google-account-linked.v1";
    public static final String GOOGLE_ACCOUNT_UNLINKED_ROUTING_KEY = "user.google-account-unlinked.v1";
    public static final String PASSWORD_RESET_GOOGLE_ONLY_ROUTING_KEY = "user.password-reset-google-only.v1";
    public static final String EMAIL_VERIFICATION_REQUESTED_ROUTING_KEY = "user.email-verification-requested.v1";
    public static final String EMAIL_VERIFICATION_STATE_ROUTING_KEY = "user.email-verification-state.v1";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted.v1";

    private UserMessaging() { }
}
