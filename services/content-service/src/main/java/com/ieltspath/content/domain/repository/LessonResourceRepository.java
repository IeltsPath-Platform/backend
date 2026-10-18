package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.LessonResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lesson resources plus the lesson's Markdown summary, which is a column of the lesson itself. */
public interface LessonResourceRepository {
    LessonResource save(LessonResource resource);

    Optional<LessonResource> findById(UUID id);

    /** The lesson's resources by type, then order. */
    List<LessonResource> findByLessonId(UUID lessonId);

    void delete(LessonResource resource);

    /** The stored summary, or {@code null} when the lesson has none. The lesson must exist. */
    String findSummaryMarkdown(UUID lessonId);

    void updateSummaryMarkdown(UUID lessonId, String summaryMarkdown);
}
