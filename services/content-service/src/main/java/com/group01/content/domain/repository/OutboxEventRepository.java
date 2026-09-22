package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    Optional<OutboxEvent> findById(UUID id);
    List<OutboxEvent> findPendingEvents(int limit);
}

