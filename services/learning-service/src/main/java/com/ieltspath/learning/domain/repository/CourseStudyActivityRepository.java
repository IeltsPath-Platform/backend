package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.CourseStudyActivity;
import java.util.Optional;
import java.util.UUID;

public interface CourseStudyActivityRepository {
    Optional<CourseStudyActivity> find(UUID userId, UUID courseId);
    boolean hasLegacyStudy(UUID userId, UUID courseId);
    void save(CourseStudyActivity activity);
}
