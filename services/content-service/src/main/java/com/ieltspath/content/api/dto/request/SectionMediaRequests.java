package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SectionMediaRequests {

    private SectionMediaRequests() {
    }

    public record Passage(@NotBlank @Size(max = 50_000) String text) {
    }

    /** {@code mediaReference} is an object key, either from an audio upload or an existing seeded file. */
    public record Audio(@NotBlank @Size(max = 300) String mediaReference, @NotNull Integer durationSeconds,
                        @Size(max = 50_000) String transcript) {
    }
}
