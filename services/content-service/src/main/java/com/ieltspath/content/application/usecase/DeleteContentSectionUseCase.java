package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeleteContentSectionUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public DeleteContentSectionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    /** Removes the section with its question rows. Assets it linked stay: a published version may still use them. */
    public void execute(UUID packageId, UUID versionId, UUID sectionId) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        pkg.requireSectionInVersion(versionId, sectionId);
        pkg.requireVersion(versionId).removeSection(sectionId);
        contentPackageRepository.save(pkg);
    }
}
