package com.group01.assessment.api.dto.response;

import com.group01.assessment.application.result.AttemptResponseResult;

import java.time.Instant;
import java.util.UUID;

public record AttemptResponseResponse(
        UUID id,
        UUID attemptItemId,
        String payload,
        int schemaVersion,
        long revision,
        Instant savedAt,
        Instant submittedAt
) {
    public static AttemptResponseResponse from(AttemptResponseResult result) {
        return new AttemptResponseResponse(
                result.id(),
                result.attemptItemId(),
                result.payload(),
                result.schemaVersion(),
                result.revision(),
                result.savedAt(),
                result.submittedAt()
        );
    }
}
