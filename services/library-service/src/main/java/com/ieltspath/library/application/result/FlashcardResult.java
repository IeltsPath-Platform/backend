package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardResult(UUID id, UUID userId, FlashcardSourceType sourceType, UUID vocabularySenseId,
                              UUID sourceReferenceId, String highlightedText, String front, String back,
                              LibraryStatus status, Instant createdAt, Instant updatedAt, int leitnerBox,
                              Instant nextReviewAt, Instant lastReviewedAt, int reviewCount, String ipa,
                              String pronunciationAudioReference, String exampleSentence) {
    public static FlashcardResult from(Flashcard flashcard) {
        return new FlashcardResult(flashcard.getId(), flashcard.getUserId(), flashcard.getSourceType(),
                flashcard.getVocabularySenseId(), flashcard.getSourceReferenceId(), flashcard.getHighlightedText(),
                flashcard.getFront(), flashcard.getBack(), flashcard.getStatus(), flashcard.getCreatedAt(),
                flashcard.getUpdatedAt(), flashcard.getLeitnerBox(), flashcard.getNextReviewAt(),
                flashcard.getLastReviewedAt(), flashcard.getReviewCount(), null, null, null);
    }

    public FlashcardResult withVocabularyMetadata(FlashcardVocabularyMetadata metadata) {
        if (metadata == null) {
            return this;
        }
        return new FlashcardResult(id, userId, sourceType, vocabularySenseId, sourceReferenceId, highlightedText,
                front, back, status, createdAt, updatedAt, leitnerBox, nextReviewAt, lastReviewedAt, reviewCount,
                metadata.ipa(), metadata.pronunciationAudioReference(), metadata.exampleSentence());
    }
}
