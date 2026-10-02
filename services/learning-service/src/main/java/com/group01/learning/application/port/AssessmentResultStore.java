package com.group01.learning.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Formal results applied to a learner; callers hold the user's advisory lock. */
public interface AssessmentResultStore {
    Optional<Integer> appliedVersion(UUID userId, UUID attemptId);

    void recordVersion(UUID userId, UUID attemptId, int resultVersion);

    /** Removes the evidence of earlier versions of a regraded attempt; lesson and review evidence stay. */
    void removeAttemptEvidence(UUID userId, UUID attemptId);

    void appendEvidence(UUID userId, UUID attemptId, int resultVersion, List<Evidence> evidence);

    /** The open assignment of this package version that was given before the attempt completed. */
    Optional<OpenAssignment> findOpenAssignment(UUID userId, UUID packageVersionId, Instant completedAt);

    void consumeAssignment(UUID assignmentId, UUID attemptId, double percent);

    /** Passing is one way: an already passed topic keeps its first pass time. */
    void passTopic(UUID userId, UUID topicId);

    record Evidence(UUID knowledgePointId, boolean correct, UUID sourceReferenceId) {}
    record OpenAssignment(UUID assignmentId, UUID topicId) {}
}
