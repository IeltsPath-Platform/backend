package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateFlashcardRequest(
        @NotNull FlashcardSourceType sourceType,
        UUID vocabularySenseId,
        UUID sourceReferenceId,
        @Size(max = 2000) String highlightedText,
        @NotBlank @Size(max = 4000) String front,
        @NotBlank @Size(max = 4000) String back,
        @NotNull LibraryStatus status
) {
}
