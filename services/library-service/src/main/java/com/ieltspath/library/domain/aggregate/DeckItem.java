package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeckItem(
        UUID deckId,
        UUID flashcardId,
        Integer sortOrder,
        Instant addedAt,
        String front,
        String back,
        FlashcardSourceType sourceType,
        LibraryStatus status,
        String imageKey,
        String exampleSentence,
        Integer leitnerBox,
        UUID vocabularySenseId,
        String ipa
) {
    public DeckItem(
            UUID deckId,
            UUID flashcardId,
            Integer sortOrder,
            Instant addedAt,
            String front,
            String back,
            FlashcardSourceType sourceType,
            LibraryStatus status,
            String imageKey,
            String exampleSentence,
            short leitnerBox,
            UUID vocabularySenseId
    ) {
        this(deckId, flashcardId, sortOrder, addedAt, front, back, sourceType, status,
                imageKey, exampleSentence, (int) leitnerBox, vocabularySenseId, null);
    }

    public DeckItem(
            UUID deckId,
            UUID flashcardId,
            Integer sortOrder,
            Instant addedAt,
            String front,
            String back,
            FlashcardSourceType sourceType,
            LibraryStatus status,
            String imageKey,
            String exampleSentence,
            Integer leitnerBox,
            UUID vocabularySenseId
    ) {
        this(deckId, flashcardId, sortOrder, addedAt, front, back, sourceType, status,
                imageKey, exampleSentence, leitnerBox, vocabularySenseId, null);
    }

    public DeckItem(
            UUID deckId,
            UUID flashcardId,
            Integer sortOrder,
            Instant addedAt,
            String front,
            String back,
            FlashcardSourceType sourceType,
            LibraryStatus status,
            String imageKey,
            String exampleSentence
    ) {
        this(deckId, flashcardId, sortOrder, addedAt, front, back, sourceType, status,
                imageKey, exampleSentence, null, null, null);
    }

    public DeckItem(
            UUID deckId,
            UUID flashcardId,
            Integer sortOrder,
            Instant addedAt,
            String front,
            String back,
            FlashcardSourceType sourceType,
            LibraryStatus status
    ) {
        this(deckId, flashcardId, sortOrder, addedAt, front, back, sourceType, status,
                null, null, null, null, null);
    }

    public DeckItem withMetadata(String ipa, String fallbackSentence) {
        return new DeckItem(
                deckId, flashcardId, sortOrder, addedAt, front, back, sourceType, status,
                imageKey,
                exampleSentence != null ? exampleSentence : fallbackSentence,
                leitnerBox,
                vocabularySenseId,
                ipa
        );
    }
}
