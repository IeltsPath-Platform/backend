package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.aggregate.DeckItem;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeckItemResult(
        UUID deckId,
        UUID flashcardId,
        Integer sortOrder,
        Instant addedAt,
        String front,
        String back,
        FlashcardSourceType sourceType,
        LibraryStatus status,
        String imageUrl,
        String exampleSentence,
        Integer leitnerBox,
        String ipa
) {
    public DeckItemResult(
            UUID deckId,
            UUID flashcardId,
            Integer sortOrder,
            Instant addedAt,
            String front,
            String back,
            FlashcardSourceType sourceType,
            LibraryStatus status,
            String imageUrl,
            String exampleSentence
    ) {
        this(deckId, flashcardId, sortOrder, addedAt, front, back, sourceType, status,
                imageUrl, exampleSentence, null, null);
    }

    public static DeckItemResult from(DeckItem item, String imageUrl) {
        return new DeckItemResult(
                item.deckId(),
                item.flashcardId(),
                item.sortOrder(),
                item.addedAt(),
                item.front(),
                item.back(),
                item.sourceType(),
                item.status(),
                imageUrl,
                item.exampleSentence(),
                item.leitnerBox(),
                item.ipa()
        );
    }

    public static DeckItemResult from(DeckItem item) {
        return from(item, null);
    }
}
