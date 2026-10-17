package com.ieltspath.learning.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LearningProgressReader {
    /** At most 101 courses, to detect overflow without truncating denominators silently. */
    List<CourseActivity> startedCourses(UUID userId);
    Set<UUID> completedLessons(UUID userId, List<UUID> lessonIds);
    record CourseActivity(UUID courseId, Instant startedAt, Instant lastStudiedAt) {}
}
