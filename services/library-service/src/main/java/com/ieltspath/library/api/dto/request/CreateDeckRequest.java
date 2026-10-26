package com.ieltspath.library.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @Size(max = 300) String coverImageKey
) {
    public CreateDeckRequest(String name, String description) {
        this(name, description, null);
    }
}
