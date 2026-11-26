package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.aggregate.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface SubscriptionRepository {

    Optional<Subscription> findById(UUID id);

    Optional<Subscription> findActiveByUserId(UUID userId);

    List<Subscription> findByUserId(UUID userId);

    void lockUserSlot(UUID userId);

    int expireLapsed(UUID userId, Instant now);

    int expireAllLapsed(Instant now);

    Subscription save(Subscription subscription);
}
