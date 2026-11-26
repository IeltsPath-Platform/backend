package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.domain.aggregate.Subscription;
import com.ieltspath.access.domain.repository.SubscriptionRepository;
import com.ieltspath.access.infrastructure.persistence.entity.SubscriptionJpaEntity;
import com.ieltspath.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.ieltspath.access.infrastructure.persistence.repository.SubscriptionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final AccessPersistenceMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository subscriptionJpaRepository, AccessPersistenceMapper mapper,
                                         JdbcTemplate jdbcTemplate) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
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
    public void lockUserSlot(UUID userId) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))",
                resultSet -> null, userId.toString());
    }

    @Override
    public int expireLapsed(UUID userId, Instant now) {
        return subscriptionJpaRepository.expireLapsedByUserId(userId, now);
    }

    @Override
    public int expireAllLapsed(Instant now) {
        return subscriptionJpaRepository.expireAllLapsed(now);
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = mapper.toEntity(subscription);
        SubscriptionJpaEntity saved = subscriptionJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
