package com.ieltspath.learning.application.result;

import java.util.List;
import java.util.UUID;

/**
 * The practice tests of the catalog a learner has finished. A test is known by its package and part ({@code partNo} is
 * null for a full test), the same key the catalog filters by; {@code submissionId} is the graded answer to open.
 */
public record PracticeProgressResult(List<Key> completed) {
    public record Key(UUID packageId, Integer partNo, UUID submissionId) {}
}
