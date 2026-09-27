package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.domain.entity.ItemResultKnowledgeJudgment;
import com.group01.assessment.domain.repository.ItemResultKnowledgeJudgmentRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.ItemResultKnowledgeJudgmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class ItemResultKnowledgeJudgmentRepositoryAdapter implements ItemResultKnowledgeJudgmentRepository {
    private final ItemResultKnowledgeJudgmentJpaRepository repository;
    private final AssessmentPersistenceMapper mapper;

    public ItemResultKnowledgeJudgmentRepositoryAdapter(ItemResultKnowledgeJudgmentJpaRepository repository,
                                                        AssessmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ItemResultKnowledgeJudgment> saveAll(List<ItemResultKnowledgeJudgment> values) {
        return repository.saveAll(values.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ItemResultKnowledgeJudgment> findByItemResultIds(Collection<UUID> itemResultIds) {
        if (itemResultIds.isEmpty()) return List.of();
        return repository.findByItemResultIds(itemResultIds).stream().map(mapper::toDomain).toList();
    }
}
