package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.PracticeTestAssignment;
import java.time.Instant;
import java.util.*;

public interface PracticeTestAssignmentRepository {
    List<PracticeTestAssignment> findByCourse(UUID userId, UUID courseId);
    Optional<PracticeTestAssignment> find(UUID userId, UUID packageId);
    Optional<PracticeTestAssignment> findOpenForAttempt(UUID userId, UUID packageVersionId, Instant completedAt);
    void save(PracticeTestAssignment assignment);
}
