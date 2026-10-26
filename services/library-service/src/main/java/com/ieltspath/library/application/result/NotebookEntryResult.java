package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.UnderstandingLevel;

import java.time.Instant;
import java.util.UUID;

public record NotebookEntryResult(UUID flashcardId, UUID vocabularySenseId, UUID vocabularyItemId,
        String lemma, String ipa, String pronunciationAudioReference, String partOfSpeech,
        String vietnameseMeaning, String englishDefinition, String exampleSentence, String imageUrl,
        UnderstandingLevel level, int leitnerBox, int reviewCount, Instant lastReviewedAt,
        Instant nextReviewAt, Instant createdAt, String note) {}
