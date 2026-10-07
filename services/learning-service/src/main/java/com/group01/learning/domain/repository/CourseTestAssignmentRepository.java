package com.group01.learning.domain.repository;

import com.group01.learning.domain.aggregate.CourseTestAssignment;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CourseTestAssignmentRepository {
    Optional<CourseTestAssignment> findOpen(UUID userId, UUID courseId);
    Optional<CourseTestAssignment> findOpenForAttempt(UUID userId, UUID packageVersionId, Instant completedAt);
    Map<UUID, Instant> lastConsumedAt(UUID userId, UUID courseId);
    void save(CourseTestAssignment assignment);
}
