package com.group01.access.domain.repository;

import com.group01.access.domain.entity.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {

    Optional<OutboxEvent> findById(UUID id);

    List<OutboxEvent> findPendingEvents(int limit);

    OutboxEvent save(OutboxEvent event);
}
