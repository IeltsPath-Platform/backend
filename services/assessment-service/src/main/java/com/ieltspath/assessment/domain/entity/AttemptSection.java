package com.ieltspath.assessment.domain.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code startedAt} is set the first time the learner opens the section and {@code completedAt} once they finish it;
 * a completed section takes no more responses.
 */
public record AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot,
                             Instant startedAt, Instant completedAt) {
    public AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot) {
        this(id, attemptId, contentSectionId, sortOrder, snapshot, null, null);
    }

    public boolean completed() {
        return completedAt != null;
    }

    /** Keeps the first opening: reopening a section after a break does not reset its clock. */
    public AttemptSection start(Instant at) {
        return startedAt != null || completed() ? this
                : new AttemptSection(id, attemptId, contentSectionId, sortOrder, snapshot, at, null);
    }

    public AttemptSection complete(Instant at) {
        return completed() ? this : new AttemptSection(id, attemptId, contentSectionId, sortOrder, snapshot, startedAt, at);
    }
}
