package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    Optional<OutboxEvent> findById(UUID id);
    List<OutboxEvent> findPendingEvents(int limit);
}

