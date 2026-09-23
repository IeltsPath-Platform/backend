package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.aggregate.Subscription;
import com.group01.access.domain.repository.SubscriptionRepository;
import com.group01.access.infrastructure.persistence.entity.SubscriptionJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.SubscriptionJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final AccessPersistenceMapper mapper;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository subscriptionJpaRepository, AccessPersistenceMapper mapper) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return subscriptionJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findActiveByUserId(UUID userId) {
        return subscriptionJpaRepository.findActiveByUserId(userId, Instant.now()).map(mapper::toDomain);
    }

    @Override
    public List<Subscription> findByUserId(UUID userId) {
        return subscriptionJpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = mapper.toEntity(subscription);
        SubscriptionJpaEntity saved = subscriptionJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
