package com.ieltspath.assessment.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * {@code elapsedMs} is optional: the learner's running time on this item, in milliseconds. Older clients omit it.
 */
public record SaveAttemptResponseRequest(@NotNull String payload, int schemaVersion, long expectedRevision,
                                         @PositiveOrZero Long elapsedMs) {

    public SaveAttemptResponseRequest(String payload, int schemaVersion, long expectedRevision) {
        this(payload, schemaVersion, expectedRevision, null);
    }
}
