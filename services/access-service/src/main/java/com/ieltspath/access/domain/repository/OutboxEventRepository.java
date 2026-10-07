package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.entity.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {

    Optional<OutboxEvent> findById(UUID id);

    List<OutboxEvent> findPendingEvents(int limit);

    OutboxEvent save(OutboxEvent event);
}
