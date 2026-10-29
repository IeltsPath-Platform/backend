package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Starts the next draft of a package from the content of one of its versions, so the author changes what is there
 * instead of building the package again. A published version is never edited; the changes go into the new draft.
 */
@Service
@Transactional
public class CopyPackageVersionToDraftUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public CopyPackageVersionToDraftUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    /** Locks the package, like any new draft, so two requests cannot both add one. */
    public AuthorVersionResult execute(UUID packageId, UUID sourceVersionId) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        ContentPackageVersion source = pkg.requireVersion(sourceVersionId);
        ContentPackageVersion draft = pkg.createDraftVersion(source.getRulesJson());
        contentPackageRepository.save(pkg);
        contentPackageRepository.copyVersionContent(source.getId(), draft.getId());
        return AuthorVersionResult.of(draft);
    }
}
