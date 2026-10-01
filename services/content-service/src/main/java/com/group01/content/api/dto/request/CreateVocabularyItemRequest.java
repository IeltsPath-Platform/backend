package com.group01.content.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVocabularyItemRequest(
        @NotBlank(message = "lemma is required")
        @Size(max = 255, message = "lemma must not exceed 255 characters")
        String lemma,

        String ipa,
        String pronunciationAudioReference
) {}

