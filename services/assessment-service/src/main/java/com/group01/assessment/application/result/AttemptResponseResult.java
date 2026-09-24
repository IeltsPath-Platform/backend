package com.group01.assessment.application.result;

import java.time.Instant;
import java.util.UUID;

public record AttemptResponseResult(
        UUID id,
        UUID attemptItemId,
        String payload,
        int schemaVersion,
        long revision,
        Instant savedAt,
        Instant submittedAt
) {
}
