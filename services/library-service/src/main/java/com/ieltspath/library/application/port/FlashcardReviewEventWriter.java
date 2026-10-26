package com.ieltspath.library.application.port;

import java.time.Instant;
import java.util.UUID;

public interface FlashcardReviewEventWriter {
    void append(UUID userId, UUID flashcardId, UUID vocabularySenseId, UUID sessionId, boolean correct, Instant reviewedAt);
}
