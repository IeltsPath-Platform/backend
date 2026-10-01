package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.repository.VocabularyRepository;
import com.group01.content.infrastructure.persistence.mapper.VocabularyPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.VocabularyItemJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VocabularyRepositoryAdapter implements VocabularyRepository {

    private final VocabularyItemJpaRepository vocabularyItemJpaRepository;
    private final VocabularyPersistenceMapper mapper;

    public VocabularyRepositoryAdapter(VocabularyItemJpaRepository vocabularyItemJpaRepository,
                                       VocabularyPersistenceMapper mapper) {
        this.vocabularyItemJpaRepository = vocabularyItemJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public VocabularyItem save(VocabularyItem item) {
        var entity = mapper.toEntity(item);
        var saved = vocabularyItemJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<VocabularyItem> findById(UUID id) {
        return vocabularyItemJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<VocabularyItem> findByIds(List<UUID> ids) {
        return vocabularyItemJpaRepository.findAllByIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<VocabularyItem> findByNormalizedLemma(String normalizedLemma) {
        return vocabularyItemJpaRepository.findByNormalizedLemma(normalizedLemma).map(mapper::toDomain);
    }

    @Override
    public List<VocabularyItem> searchByLemma(String query) {
        return vocabularyItemJpaRepository.searchByLemma(query).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByNormalizedLemma(String normalizedLemma) {
        return vocabularyItemJpaRepository.existsByNormalizedLemma(normalizedLemma);
    }
}

