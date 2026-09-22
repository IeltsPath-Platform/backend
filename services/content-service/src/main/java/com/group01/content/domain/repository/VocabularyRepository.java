package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.VocabularyItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyRepository {
    VocabularyItem save(VocabularyItem item);
    Optional<VocabularyItem> findById(UUID id);
    Optional<VocabularyItem> findByNormalizedLemma(String normalizedLemma);
    List<VocabularyItem> searchByLemma(String query);
    boolean existsByNormalizedLemma(String normalizedLemma);
}

