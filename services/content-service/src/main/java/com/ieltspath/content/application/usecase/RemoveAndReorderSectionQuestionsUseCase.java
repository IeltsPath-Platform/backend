package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Removing a question and reordering a section's questions both rewrite the section's orders as one set, so the
 * uniqueness check is deferred to commit.
 */
@Service
@Transactional
public class RemoveAndReorderSectionQuestionsUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public RemoveAndReorderSectionQuestionsUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public void remove(UUID packageId, UUID versionId, UUID sectionId, UUID sectionQuestionId) {
        ContentPackage pkg = lockDraft(packageId, versionId);
        pkg.requireSectionInVersion(versionId, sectionId).removeQuestion(sectionQuestionId);
        contentPackageRepository.save(pkg);
    }

    public void reorder(UUID packageId, UUID versionId, UUID sectionId, List<UUID> sectionQuestionIds) {
        ContentPackage pkg = lockDraft(packageId, versionId);
        pkg.requireSectionInVersion(versionId, sectionId).reorderQuestions(sectionQuestionIds);
        contentPackageRepository.save(pkg);
    }

    private ContentPackage lockDraft(UUID packageId, UUID versionId) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        contentPackageRepository.deferQuestionOrderCheck();
        return pkg;
    }
}
