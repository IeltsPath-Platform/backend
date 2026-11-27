package com.ieltspath.user.application.port;

import java.util.Optional;
import java.util.UUID;

/** Cleans up user-owned identity and profile records during self-service account deletion. */
public interface AccountDeletionCleanupPort {
    Optional<String> anonymizeProfileAndDeleteIdentityData(UUID userId);

    /** Records an avatar object to remove from storage; a background job retries until it is gone. */
    void queueAvatarDeletion(UUID userId, String objectKey);
}
