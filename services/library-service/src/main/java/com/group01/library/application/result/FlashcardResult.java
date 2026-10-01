package com.group01.library.application.result;

import com.group01.library.domain.aggregate.Flashcard;
import com.group01.library.domain.vo.FlashcardSourceType;
import com.group01.library.domain.vo.LibraryStatus;

import java.time.Instant;
import java.util.UUID;

public record FlashcardResult(UUID id, UUID userId, FlashcardSourceType sourceType, UUID vocabularySenseId,
                              UUID sourceReferenceId, String highlightedText, String front, String back,
                              LibraryStatus status, Instant createdAt, Instant updatedAt) {
    public static FlashcardResult from(Flashcard flashcard) {
        return new FlashcardResult(flashcard.getId(), flashcard.getUserId(), flashcard.getSourceType(),
                flashcard.getVocabularySenseId(), flashcard.getSourceReferenceId(), flashcard.getHighlightedText(),
                flashcard.getFront(), flashcard.getBack(), flashcard.getStatus(), flashcard.getCreatedAt(),
                flashcard.getUpdatedAt());
    }
}
