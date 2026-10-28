package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.PartOfSpeech;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkVocabularyRequest(
        @NotEmpty(message = "entries is required")
        @Size(max = 200, message = "entries must not exceed 200 items")
        List<@NotNull @Valid Entry> entries
) {

    public record Entry(
            @NotBlank(message = "lemma is required")
            @Size(max = 255, message = "lemma must not exceed 255 characters")
            String lemma,

            @Size(max = 255, message = "ipa must not exceed 255 characters")
            String ipa,

            @NotNull(message = "partOfSpeech is required")
            PartOfSpeech partOfSpeech,

            @Size(max = 2000, message = "englishDefinition must not exceed 2000 characters")
            String englishDefinition,

            @NotBlank(message = "vietnameseMeaning is required")
            @Size(max = 2000, message = "vietnameseMeaning must not exceed 2000 characters")
            String vietnameseMeaning,

            @NotBlank(message = "exampleSentence is required")
            @Size(max = 2000, message = "exampleSentence must not exceed 2000 characters")
            String exampleSentence
    ) {}
}
