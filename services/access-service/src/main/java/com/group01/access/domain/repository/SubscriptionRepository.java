package com.group01.access.domain.repository;

import com.group01.access.domain.aggregate.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {

    Optional<Subscription> findById(UUID id);

    Optional<Subscription> findActiveByUserId(UUID userId);

    List<Subscription> findByUserId(UUID userId);

    Subscription save(Subscription subscription);
}
