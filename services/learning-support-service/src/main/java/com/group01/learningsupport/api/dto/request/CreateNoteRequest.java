package com.group01.learningsupport.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20000) String body
) {
}
