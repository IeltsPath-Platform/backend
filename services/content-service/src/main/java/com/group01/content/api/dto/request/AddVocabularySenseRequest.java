package com.group01.content.api.dto.request;

import com.group01.content.domain.vo.PartOfSpeech;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddVocabularySenseRequest(
        @NotNull(message = "partOfSpeech is required")
        PartOfSpeech partOfSpeech,

        String englishDefinition,

        @NotBlank(message = "vietnameseMeaning is required")
        String vietnameseMeaning,

        @NotBlank(message = "exampleSentence is required")
        String exampleSentence,

        String imageUrl,
        int sortOrder
) {}

