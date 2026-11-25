package com.ieltspath.content.application.port;

import com.ieltspath.content.domain.vo.CourseImportModel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Where an author's kept course workbooks live. Every read and write is scoped to its author. */
public interface CourseImportDraftStore {

    String DRAFT = "DRAFT";
    String IMPORTED = "IMPORTED";

    record Draft(UUID id, UUID authorId, String filename, String status, UUID importedCourseId,
                 CourseImportModel model, Instant createdAt, Instant updatedAt) {
    }

    /** One line of the author's list; the course is named by the first row of the Course sheet. */
    record Summary(UUID id, String filename, String status, UUID importedCourseId, String courseCode,
                   String courseName, Instant updatedAt) {
    }

    void insert(Draft draft);

    Optional<Draft> find(UUID author, UUID id);

    void updateContent(UUID author, UUID id, CourseImportModel model, Instant now);

    void markImported(UUID author, UUID id, UUID courseId, Instant now);

    boolean delete(UUID author, UUID id);

    List<Summary> list(UUID author);

    int countDrafts(UUID author);
}
