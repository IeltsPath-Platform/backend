package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.OutboxEvent;
import com.ieltspath.content.domain.repository.OutboxEventRepository;
import com.ieltspath.content.infrastructure.persistence.mapper.OutboxEventPersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final OutboxEventPersistenceMapper mapper;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository,
                                        OutboxEventPersistenceMapper mapper) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        var entity = mapper.toEntity(event);
        var saved = outboxEventJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return outboxEventJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OutboxEvent> findPendingEvents(int limit) {
        return outboxEventJpaRepository.findPendingEvents(PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain).toList();
    }
}

