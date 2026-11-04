package com.ieltspath.library.infrastructure.persistence.adapter;

import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.VocabularySenseCard;
import com.ieltspath.library.infrastructure.persistence.JpaSupport;
import com.ieltspath.library.infrastructure.persistence.mapper.VocabularyPersistenceMapper;
import com.ieltspath.library.infrastructure.persistence.repository.VocabularyItemJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VocabularyRepositoryAdapter implements VocabularyRepository {

    /** The most words one search returns: the box shows the best matches, not the whole catalog. */
    static final int SEARCH_LIMIT = 50;

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
        // Flushed so the row exists for JDBC writes later in the same transaction (the dictionary columns).
        return mapper.toDomain(JpaSupport.flush(vocabularyItemJpaRepository, entity));
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
    public List<VocabularySenseCard> findActiveSensesByIds(List<UUID> senseIds) {
        return vocabularyItemJpaRepository.findActiveSenseCardsByIds(senseIds).stream()
                .map(card -> new VocabularySenseCard(
                        card.getSenseId(),
                        card.getVocabularyItemId(),
                        card.getLemma(),
                        card.getIpa(),
                        card.getPronunciationAudioReference(),
                        card.getPartOfSpeech(),
                        card.getEnglishDefinition(),
                        card.getVietnameseMeaning(),
                        card.getExampleSentence(),
                        card.getImageUrl()
                ))
                .toList();
    }

    @Override
    public Optional<VocabularyItem> findByNormalizedLemma(String normalizedLemma) {
        return vocabularyItemJpaRepository.findByNormalizedLemma(normalizedLemma).map(mapper::toDomain);
    }

    @Override
    public List<VocabularyItem> searchByLemma(String query) {
        String text = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        // An empty box used to return the whole catalog; it now asks for nothing.
        if (text.isEmpty()) return List.of();
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        // Ids first, senses second: a LIMIT on a fetch of the senses collection would page in memory.
        List<UUID> ids = vocabularyItemJpaRepository.searchIdsByLemma(text, escaped + "%", "%" + escaped + "%", SEARCH_LIMIT);
        if (ids.isEmpty()) return List.of();
        java.util.Map<UUID, VocabularyItem> byId = new java.util.HashMap<>();
        vocabularyItemJpaRepository.findAllByIdIn(ids).forEach(entity -> byId.put(entity.getId(), mapper.toDomain(entity)));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public List<VocabularyItem> findByNormalizedLemmas(java.util.Collection<String> normalizedLemmas) {
        if (normalizedLemmas == null || normalizedLemmas.isEmpty()) return List.of();
        return vocabularyItemJpaRepository.findAllByNormalizedLemmaIn(normalizedLemmas).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByNormalizedLemma(String normalizedLemma) {
        return vocabularyItemJpaRepository.existsByNormalizedLemma(normalizedLemma);
    }
}
