package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.FlashcardSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFlashcardRequest(
        @NotNull FlashcardSourceType sourceType,
        UUID vocabularySenseId,
        UUID sourceReferenceId,
        @Size(max = 2000) String highlightedText,
        @NotBlank @Size(max = 4000) String front,
        @NotBlank @Size(max = 4000) String back,
        @Size(max = 300) String imageKey,
        @Size(max = 1000) String exampleSentence
) {
    public CreateFlashcardRequest(
            FlashcardSourceType sourceType,
            UUID vocabularySenseId,
            UUID sourceReferenceId,
            String highlightedText,
            String front,
            String back
    ) {
        this(sourceType, vocabularySenseId, sourceReferenceId, highlightedText, front, back, null, null);
    }
}
