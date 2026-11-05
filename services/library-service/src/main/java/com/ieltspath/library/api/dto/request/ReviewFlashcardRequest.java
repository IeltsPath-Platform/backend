package com.ieltspath.library.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewFlashcardRequest(
        @NotNull(message = "correct is required") Boolean correct,
        @NotNull(message = "requestId is required") UUID requestId,
        UUID sessionId
) {
}
