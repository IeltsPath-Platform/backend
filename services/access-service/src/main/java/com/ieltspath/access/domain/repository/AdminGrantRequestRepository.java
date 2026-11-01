package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.AdminGrantRequest;

import java.util.Optional;
import java.util.UUID;

public interface AdminGrantRequestRepository {
    /** Inserts only if the key is unused; false means another transaction reserved it first. */
    boolean reserve(String idempotencyKey, String requestHash, UUID subscriptionId);

    Optional<AdminGrantRequest> find(String idempotencyKey);
}
