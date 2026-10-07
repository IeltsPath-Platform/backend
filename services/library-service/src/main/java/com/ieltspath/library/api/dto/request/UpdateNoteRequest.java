package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.LibraryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20000) String body,
        @NotNull LibraryStatus status
) {
}
