package com.group01.assessment.domain.entity;

import java.time.Instant;
import java.util.UUID;

public record AttemptResponse(UUID id, UUID attemptItemId, String payload, int schemaVersion,
                              long revision, Instant savedAt, Instant submittedAt, long lockVersion) {
    public AttemptResponse(UUID id, UUID attemptItemId, String payload, int schemaVersion, long revision, Instant savedAt, Instant submittedAt) {
        this(id, attemptItemId, payload, schemaVersion, revision, savedAt, submittedAt, 0);
    }
}
