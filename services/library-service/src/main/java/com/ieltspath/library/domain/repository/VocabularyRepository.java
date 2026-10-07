package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.VocabularyItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyRepository {
    VocabularyItem save(VocabularyItem item);
    Optional<VocabularyItem> findById(UUID id);
    List<VocabularyItem> findByIds(List<UUID> ids);
    Optional<VocabularyItem> findByNormalizedLemma(String normalizedLemma);
    List<VocabularyItem> searchByLemma(String query);
    boolean existsByNormalizedLemma(String normalizedLemma);
}
