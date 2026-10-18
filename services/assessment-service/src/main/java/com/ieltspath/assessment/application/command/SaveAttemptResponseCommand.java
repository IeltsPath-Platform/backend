package com.ieltspath.assessment.application.command;

import java.util.UUID;

/** {@code elapsedMs} is null when the client did not report time for this save. */
public record SaveAttemptResponseCommand(UUID userId, UUID attemptId, UUID itemId,
                                         String payload, int schemaVersion, long expectedRevision, Long elapsedMs) {

    public SaveAttemptResponseCommand(UUID userId, UUID attemptId, UUID itemId, String payload, int schemaVersion,
                                      long expectedRevision) {
        this(userId, attemptId, itemId, payload, schemaVersion, expectedRevision, null);
    }
}
