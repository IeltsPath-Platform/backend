package com.ieltspath.library.application.query;

import com.ieltspath.library.domain.exception.InvalidDataException;

import java.util.UUID;

public record DueFlashcardsQuery(UUID deckId, int page, int limit) {
    public DueFlashcardsQuery {
        if (page < 0 || limit < 1 || limit > 50) {
            throw new InvalidDataException("Phân trang flashcard không hợp lệ");
        }
    }
}
