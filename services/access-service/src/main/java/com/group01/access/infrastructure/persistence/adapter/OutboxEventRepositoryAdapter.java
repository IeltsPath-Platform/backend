package com.group01.access.infrastructure.persistence.adapter;

import com.group01.access.domain.entity.OutboxEvent;
import com.group01.access.domain.repository.OutboxEventRepository;
import com.group01.access.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.group01.access.infrastructure.persistence.mapper.AccessPersistenceMapper;
import com.group01.access.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final AccessPersistenceMapper mapper;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository, AccessPersistenceMapper mapper) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return outboxEventJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventJpaEntity entity = mapper.toEntity(event);
        OutboxEventJpaEntity saved = outboxEventJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
