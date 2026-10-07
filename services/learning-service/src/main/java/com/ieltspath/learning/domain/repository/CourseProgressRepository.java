package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.CourseProgress;

import java.util.Map;
import java.util.UUID;

public interface CourseProgressRepository {
    Map<UUID, CourseProgress> findAll(UUID userId);
    void save(CourseProgress progress);
}
