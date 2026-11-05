package com.ieltspath.user.application.port;

public interface LoginAttemptLimiter {
    /** Rejects a pair or email currently over its shared Redis threshold. */
    void checkAllowed(String normalizedEmail, String clientIp);
    /** Records a rejected credential check and starts the progressive email wait when necessary. */
    void recordFailure(String normalizedEmail, String clientIp);
    /** Clears the failed-attempt counter for the successful email/IP pair. */
    void recordSuccess(String normalizedEmail, String clientIp);
}
