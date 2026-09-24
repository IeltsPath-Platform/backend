package com.group01.assessment.application.port;

import java.util.Optional;
import java.util.UUID;

/** Resolves the learner's active learning goal from User Service, which owns goals. */
public interface LearningGoalProvider {
    /**
     * @return the active goal id, or empty when the learner currently has no active goal.
     * Dependency failures are thrown so an attempt is never started with an unknown attribution.
     */
    Optional<UUID> findActiveGoalId(UUID userId);
}
