package com.group01.content.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddSegmentLexicalEntryRequest(
        @NotNull(message = "vocabularySenseId is required")
        UUID vocabularySenseId,

        @NotBlank(message = "surfaceText is required")
        String surfaceText,

        int startChar,
        int endChar,
        int sortOrder
) {}

