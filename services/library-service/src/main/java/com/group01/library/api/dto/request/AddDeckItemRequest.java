package com.group01.library.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddDeckItemRequest(
        @NotNull UUID flashcardId,
        Integer sortOrder
) {
}
