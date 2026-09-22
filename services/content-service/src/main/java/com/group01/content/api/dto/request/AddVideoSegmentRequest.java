package com.group01.content.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddVideoSegmentRequest(
        @Min(value = 1, message = "sequenceNo must be at least 1")
        int sequenceNo,

        @Min(value = 0, message = "startMs must not be negative")
        int startMs,

        @Min(value = 0, message = "endMs must not be negative")
        int endMs,

        @NotBlank(message = "transcript is required")
        String transcript,

        String translationVi
) {}

