package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.CourseOutcome;

import java.util.Optional;
import java.util.UUID;

public interface CourseOutcomeRepository {
    Optional<CourseOutcome> find(UUID userId, UUID courseId);

    /** Writes the decision, replacing an earlier one for the same learner and course. */
    void save(CourseOutcome outcome);
}
