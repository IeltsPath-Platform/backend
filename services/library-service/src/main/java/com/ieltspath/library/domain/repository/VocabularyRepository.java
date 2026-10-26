package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.vo.VocabularySenseCard;
import com.ieltspath.library.domain.vo.OwnedPage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyRepository {
    VocabularyItem save(VocabularyItem item);
    Optional<VocabularyItem> findById(UUID id);
    List<VocabularyItem> findByIds(List<UUID> ids);
    List<VocabularySenseCard> findActiveSensesByIds(List<UUID> senseIds);
    Optional<VocabularyItem> findByNormalizedLemma(String normalizedLemma);
    List<VocabularyItem> searchByLemma(String query);
    boolean existsByNormalizedLemma(String normalizedLemma);
    OwnedPage<VocabularyItem> browseActive(String query, int page, int size);
}
