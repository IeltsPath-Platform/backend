package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.LessonProgress;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LessonProgressRepository {
    Optional<LessonProgress> find(UUID userId, UUID lessonId);

    /** The learner's progress in the topic's lessons, keyed by lesson id. */
    Map<UUID, LessonProgress> findByTopic(UUID userId, UUID topicId);

    List<LessonProgress> findCompleted(UUID userId);

    Map<UUID, Integer> completedCountsByTopic(UUID userId);

    void save(LessonProgress progress);
}
