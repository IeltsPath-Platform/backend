package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.CourseScoreConfig;

import java.util.Optional;
import java.util.UUID;

public interface CourseScoreConfigRepository {
    /** The configuration of one course, or the default when {@code courseId} is null; empty if none is stored. */
    Optional<CourseScoreConfig> find(UUID courseId);

    /** The course's own configuration, else the default. */
    CourseScoreConfig effective(UUID courseId);

    /** Stores the configuration for its course (the default when its course is null). */
    void save(CourseScoreConfig config, UUID updatedBy);
}
