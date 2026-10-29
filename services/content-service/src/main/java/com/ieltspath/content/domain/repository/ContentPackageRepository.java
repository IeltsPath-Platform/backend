package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.vo.PublicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPackageRepository {
    ContentPackage save(ContentPackage pkg);
    Optional<ContentPackage> findById(UUID id);

    /** Locks the package row until the transaction ends, so writes to one package's versions run one at a time. */
    Optional<ContentPackage> findByIdForUpdate(UUID id);

    /** Checks the section sort-order uniqueness at commit instead of per row, so one whole reordering can be saved. */
    void deferSectionOrderCheck();

    /** Checks the question order uniqueness within a section at commit, for removals and reorderings. */
    void deferQuestionOrderCheck();

    /**
     * Copies the sections of {@code fromVersionId}, with their questions, passages and audio, into {@code toVersionId}.
     * Saves what is waiting first, so the target version may be one that was just added to its package.
     */
    void copyVersionContent(UUID fromVersionId, UUID toVersionId);

    /** Whether a question version exists; the author may only attach versions that do. */
    boolean questionVersionExists(UUID questionVersionId);

    /**
     * Makes an existing, unattached practice set part of a lesson's Practice without rewriting the package or its
     * versions. Returns false when the package is not a practice set or already belongs to a lesson.
     */
    boolean attachPracticeSetToLesson(UUID packageId, UUID lessonId);

    Optional<ContentPackage> findByVersionId(UUID versionId);

    Optional<ContentPackage> findBySectionId(UUID sectionId);
    Optional<ContentPackage> findByCode(String code);

    List<ContentPackage> findAll(Boolean featureRequired, PublicationStatus status);
    boolean existsByCode(String code);
}
