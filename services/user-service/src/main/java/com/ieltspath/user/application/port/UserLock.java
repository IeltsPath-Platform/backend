package com.ieltspath.user.application.port;

import java.util.UUID;

/**
 * Serializes work on one account until the current transaction ends: password changes and resets, refresh-token
 * rotation and the failed-attempt counter all read then write the same user's rows. Must be called inside a transaction.
 */
public interface UserLock {
    void lock(UUID userId);

    /** Serializes first-time external account resolution without exposing its email or provider subject in logs. */
    void lockCreationKey(String key);
}
