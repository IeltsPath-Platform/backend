package com.ieltspath.user.application.port;

public interface GoogleSignInMetrics {
    void record(Result result);

    enum Result {
        SUCCESS("success"),
        INVALID_TOKEN("invalid_token"),
        EMAIL_NOT_SUPPORTED("email_not_supported"),
        NOT_ALLOWED("not_allowed"),
        CONFLICT("conflict"),
        INACTIVE("inactive"),
        UNAVAILABLE("unavailable");

        private final String tag;

        Result(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }
}
