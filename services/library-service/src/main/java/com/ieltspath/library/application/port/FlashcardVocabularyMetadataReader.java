package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.FlashcardVocabularyMetadata;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface FlashcardVocabularyMetadataReader {
    Map<UUID, FlashcardVocabularyMetadata> findBySenseIds(Collection<UUID> senseIds);
}
