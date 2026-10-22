package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonAuthoringRepository {
    boolean topicExists(UUID topicId);
    Lesson create(UUID topicId, String code, String title, String summary, String summaryMarkdown,
                                 Integer estimatedMinutes, Integer sortOrder);
    List<LessonAuthoringSnapshot> findByTopic(UUID topicId);
    Optional<LessonAuthoringSnapshot> findSnapshotById(UUID lessonId);
    Optional<Lesson> lockAndFindById(UUID lessonId);
    Lesson save(Lesson lesson);
    void reorderLessons(UUID topicId, List<UUID> lessonIds);
    List<String> publicationViolations(UUID lessonId);
}
