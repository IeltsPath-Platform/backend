package com.group01.learningsupport.application.result;

/** A saved flashcard and whether this call created it ({@code false} when the card already existed). */
public record SavedFlashcard(FlashcardResult flashcard, boolean created) {
}
