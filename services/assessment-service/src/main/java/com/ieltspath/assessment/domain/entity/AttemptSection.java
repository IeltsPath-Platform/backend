package com.ieltspath.assessment.domain.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code startedAt} is set the first time the learner opens the section and {@code completedAt} once they finish it;
 * a completed section takes no more responses.
 */
public record AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot,
                             Instant startedAt, Instant completedAt, Integer partNo, Integer timeSpentSeconds) {
    public AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot,
                          Instant startedAt, Instant completedAt, Integer partNo) {
        this(id, attemptId, contentSectionId, sortOrder, snapshot, startedAt, completedAt, partNo, null);
    }

    public AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot) {
        this(id, attemptId, contentSectionId, sortOrder, snapshot, null, null, null, null);
    }

    public AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot,
                          Instant startedAt, Instant completedAt) {
        this(id, attemptId, contentSectionId, sortOrder, snapshot, startedAt, completedAt, null, null);
    }

    public boolean completed() {
        return completedAt != null;
    }

    /** Keeps the first opening: reopening a section after a break does not reset its clock. */
    public AttemptSection start(Instant at) {
        return startedAt != null || completed() ? this
                : new AttemptSection(id, attemptId, contentSectionId, sortOrder, snapshot, at, null, partNo, timeSpentSeconds);
    }

    public AttemptSection complete(Instant at) {
        return completed() ? this : new AttemptSection(id, attemptId, contentSectionId, sortOrder, snapshot,
                startedAt, at, partNo, timeSpentSeconds);
    }

    /** The client reports how long the learner was active in the section; a day is the most a section can hold. */
    public AttemptSection withTimeSpent(int seconds) {
        return new AttemptSection(id, attemptId, contentSectionId, sortOrder, snapshot, startedAt, completedAt, partNo,
                Math.max(0, Math.min(seconds, 86_400)));
    }
}
