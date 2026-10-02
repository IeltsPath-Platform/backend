package com.group01.learning.domain.repository;

import com.group01.learning.domain.aggregate.TopicTestAssignment;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TopicTestAssignmentRepository {
    /** The learner's unconsumed assignment for the topic. */
    Optional<TopicTestAssignment> findOpen(UUID userId, UUID topicId);

    /** The newest unconsumed assignment of this package version given before the attempt completed, row-locked. */
    Optional<TopicTestAssignment> findOpenForAttempt(UUID userId, UUID packageVersionId, Instant completedAt);

    /** Latest consumption time of each package the learner has used for the topic. */
    Map<UUID, Instant> lastConsumedAt(UUID userId, UUID topicId);

    /** Inserts a new assignment or records its consumption; a consumed assignment is not changed again. */
    void save(TopicTestAssignment assignment);
}
