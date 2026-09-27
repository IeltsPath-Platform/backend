package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.domain.entity.OutboxEvent;
import com.group01.assessment.domain.repository.OutboxEventRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {
    private static final int MAX_ERROR_LENGTH = 2000;

    private final OutboxEventJpaRepository repository;
    private final AssessmentPersistenceMapper mapper;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository repository, AssessmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return mapper.toDomain(repository.save(mapper.toEntity(event)));
    }

    @Override
    public List<OutboxEvent> claimUnpublished(int limit, int maxAttempts) {
        return repository.claimUnpublished(limit, maxAttempts).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void markPublished(UUID id, Instant publishedAt) {
        repository.markPublished(id, publishedAt);
    }

    @Override
    public void recordFailure(UUID id, String error) {
        String message = error == null ? "unknown error" : error;
        repository.recordFailure(id, message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message);
    }
}
