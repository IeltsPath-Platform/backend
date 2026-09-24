package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.repository.AttemptItemKnowledgePointRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.AttemptItemKnowledgePointJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class AttemptItemKnowledgePointRepositoryAdapter implements AttemptItemKnowledgePointRepository {
    private final AttemptItemKnowledgePointJpaRepository repository;
    private final AssessmentPersistenceMapper mapper;

    public AttemptItemKnowledgePointRepositoryAdapter(AttemptItemKnowledgePointJpaRepository repository,
                                                      AssessmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<AttemptItemKnowledgePoint> saveAll(List<AttemptItemKnowledgePoint> values) {
        return repository.saveAll(values.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AttemptItemKnowledgePoint> findByAttemptItemIds(Collection<UUID> attemptItemIds) {
        if (attemptItemIds.isEmpty()) return List.of();
        return repository.findByAttemptItemIds(attemptItemIds).stream().map(mapper::toDomain).toList();
    }
}
