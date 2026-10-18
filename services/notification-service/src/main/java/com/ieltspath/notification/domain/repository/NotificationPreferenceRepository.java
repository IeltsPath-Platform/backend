package com.ieltspath.notification.domain.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {
    /** Explicitly saved choices of the recipient, keyed by event type. */
    Map<String, Boolean> findAll(UUID recipientId);

    Optional<Boolean> find(UUID recipientId, String eventType);

    void save(UUID recipientId, String eventType, boolean enabled);
}
