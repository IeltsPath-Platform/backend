package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.NotebookEntryResult;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import java.time.Instant;
import java.util.UUID;

public record NotebookEntryResponse(UUID flashcardId, UUID vocabularySenseId, UUID vocabularyItemId,
        String lemma, String ipa, String pronunciationAudioReference, String partOfSpeech,
        String vietnameseMeaning, String englishDefinition, String exampleSentence, String imageUrl,
        UnderstandingLevel level, int leitnerBox, int reviewCount, Instant lastReviewedAt,
        Instant nextReviewAt, Instant createdAt, String note, String cefrLevel, String topicName) {
    public static NotebookEntryResponse from(NotebookEntryResult r) {
        return new NotebookEntryResponse(r.flashcardId(), r.vocabularySenseId(), r.vocabularyItemId(), r.lemma(), r.ipa(),
                r.pronunciationAudioReference(), r.partOfSpeech(), r.vietnameseMeaning(), r.englishDefinition(),
                r.exampleSentence(), r.imageUrl(), r.level(), r.leitnerBox(), r.reviewCount(), r.lastReviewedAt(),
                r.nextReviewAt(), r.createdAt(), r.note(), r.cefrLevel(), r.topicName());
    }
}
