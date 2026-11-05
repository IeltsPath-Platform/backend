package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.LessonProgress;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LessonProgressRepository {
    Optional<LessonProgress> find(UUID userId, UUID lessonId);

    /** The learner's progress in the topic's lessons, keyed by lesson id. */
    Map<UUID, LessonProgress> findByTopic(UUID userId, UUID topicId);

    List<LessonProgress> findCompleted(UUID userId);

    boolean hasGrandfatheredLessonForKnowledgePoint(UUID userId, UUID knowledgePointId);

    Set<UUID> findGrandfatheredKnowledgePointIds(UUID userId);

    Map<UUID, Integer> completedCountsByTopic(UUID userId);

    /**
     * When the learner last studied each topic: the latest time they opened one of its lessons or submitted one of
     * its practice sets. Topics they never opened are absent.
     */
    Map<UUID, Instant> lastStudiedByTopic(UUID userId);

    void save(LessonProgress progress);
}
