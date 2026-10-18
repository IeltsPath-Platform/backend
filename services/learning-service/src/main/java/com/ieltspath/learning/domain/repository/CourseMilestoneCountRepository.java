package com.ieltspath.learning.domain.repository;

import java.util.Optional;
import java.util.UUID;

/** The number of practice-test milestones of each course, as Content last reported it. */
public interface CourseMilestoneCountRepository {
    /** Writes the count when the course has none or a different one; independent of the caller's transaction. */
    void saveIfChanged(UUID courseId, int count);

    Optional<Integer> find(UUID courseId);
}
