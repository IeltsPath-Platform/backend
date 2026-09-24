package com.group01.game.infrastructure.persistence.adapter;

import com.group01.game.application.port.OutboxWriter;
import com.group01.game.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.group01.game.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
public class OutboxWriterAdapter implements OutboxWriter {
    private final OutboxEventJpaRepository repository;

    public OutboxWriterAdapter(OutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        repository.save(new OutboxEventJpaEntity(UUID.randomUUID(), aggregateType, aggregateId,
                eventType, payload, Instant.now()));
    }
}
