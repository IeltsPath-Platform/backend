package com.ieltspath.library.infrastructure.persistence.repository;

import java.util.UUID;

public interface VocabularyFlashcardMetadataProjection {
    UUID getSenseId();
    String getIpa();
    String getPronunciationAudioReference();
    String getExampleSentence();
}
