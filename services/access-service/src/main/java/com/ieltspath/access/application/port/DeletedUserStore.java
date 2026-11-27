package com.ieltspath.access.application.port;

import java.time.Instant;
import java.util.UUID;

public interface DeletedUserStore {
    boolean isDeleted(UUID userId);

    void recordDeletion(UUID eventId, UUID userId, Instant occurredAt);
}
