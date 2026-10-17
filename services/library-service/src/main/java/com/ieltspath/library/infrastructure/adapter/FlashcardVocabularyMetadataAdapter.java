package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.application.result.FlashcardVocabularyMetadata;
import com.ieltspath.library.infrastructure.persistence.repository.VocabularySenseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FlashcardVocabularyMetadataAdapter implements FlashcardVocabularyMetadataReader {
    private final VocabularySenseJpaRepository repository;

    @Override
    public Map<UUID, FlashcardVocabularyMetadata> findBySenseIds(Collection<UUID> senseIds) {
        if (senseIds == null || senseIds.isEmpty()) {
            return Map.of();
        }
        return repository.findFlashcardMetadataBySenseIds(senseIds).stream()
                .map(row -> new FlashcardVocabularyMetadata(row.getSenseId(), row.getIpa(),
                        row.getPronunciationAudioReference(), row.getExampleSentence()))
                .collect(Collectors.toUnmodifiableMap(FlashcardVocabularyMetadata::senseId, metadata -> metadata));
    }
}
