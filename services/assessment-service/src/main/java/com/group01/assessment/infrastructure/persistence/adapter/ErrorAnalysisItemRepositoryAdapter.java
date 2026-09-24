package com.group01.assessment.infrastructure.persistence.adapter;

import com.group01.assessment.domain.entity.ErrorAnalysisItem;
import com.group01.assessment.domain.repository.ErrorAnalysisItemRepository;
import com.group01.assessment.infrastructure.persistence.mapper.AssessmentPersistenceMapper;
import com.group01.assessment.infrastructure.persistence.repository.ErrorAnalysisItemJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ErrorAnalysisItemRepositoryAdapter implements ErrorAnalysisItemRepository {
    private final ErrorAnalysisItemJpaRepository repository;
    private final AssessmentPersistenceMapper mapper;

    public ErrorAnalysisItemRepositoryAdapter(ErrorAnalysisItemJpaRepository repository,
                                              AssessmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ErrorAnalysisItem> saveAll(List<ErrorAnalysisItem> values) {
        return repository.saveAll(values.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ErrorAnalysisItem> findByResultId(UUID resultId) {
        return repository.findByResultId(resultId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ErrorAnalysisItem> findByIds(Set<UUID> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }
}
