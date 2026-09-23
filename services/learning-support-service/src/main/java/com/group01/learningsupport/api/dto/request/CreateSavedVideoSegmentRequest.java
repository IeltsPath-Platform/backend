package com.group01.learningsupport.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSavedVideoSegmentRequest(
        @NotNull UUID videoId,
        @NotNull UUID segmentId,
        @NotBlank @Size(max = 20000) String transcriptSnapshot,
        @Size(max = 2000) String note
) {
}
