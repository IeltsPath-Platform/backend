package com.ieltspath.notification.domain.repository;

import java.time.Instant;
import java.util.UUID;

/** Phones registered to receive push. A token belongs to at most one installation, an installation to one user. */
public interface PushDeviceRepository {
    /** Creates or takes over the installation for the user and frees the token from any other installation. */
    void register(UUID userId, String installationId, String platform, String token, Instant now);

    /** Removes the user's installation; false when it does not exist or belongs to someone else. */
    boolean revoke(UUID userId, String installationId);

    /** Removes the installation holding a token that the push provider says is no longer valid. */
    void deleteByToken(String token);
}
