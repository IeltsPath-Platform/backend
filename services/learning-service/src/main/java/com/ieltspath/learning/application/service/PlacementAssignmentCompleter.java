package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks the placement assignment a finished attempt belongs to as completed. It runs in a transaction of its own: a
 * statement that fails inside the consumer's transaction would leave PostgreSQL refusing every later statement and turn
 * the commit into a rollback, taking the learner's new band with it. Here a failure only rolls this step back.
 */
@Component
public class PlacementAssignmentCompleter {
    private final PlacementAssignmentRepository assignments;

    public PlacementAssignmentCompleter(PlacementAssignmentRepository assignments) {
        this.assignments = assignments;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID userId, UUID packageVersionId, UUID attemptId, Instant completedAt) {
        assignments.findOpenForResult(userId, packageVersionId, completedAt).ifPresent(assignment -> {
            if (assignment.complete(attemptId, completedAt)) {
                assignments.save(assignment);
            }
        });
    }
}
