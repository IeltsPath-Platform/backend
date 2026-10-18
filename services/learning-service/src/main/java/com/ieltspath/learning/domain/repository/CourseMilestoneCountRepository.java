package com.ieltspath.learning.domain.repository;

import java.util.Optional;
import java.util.UUID;

/** The number of practice-test milestones of each course, as Content last reported it. */
public interface CourseMilestoneCountRepository {
    void save(UUID courseId, int count);

    Optional<Integer> find(UUID courseId);
}
