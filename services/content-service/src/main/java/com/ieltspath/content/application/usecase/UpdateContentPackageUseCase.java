package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Changes the title and access of a package; its versions are not touched. */
@Service
@Transactional
public class UpdateContentPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public UpdateContentPackageUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public AuthorPackageResult execute(UUID packageId, String title, String requiredFeatureKey) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.updateDetails(title.trim(), requiredFeatureKey);
        return AuthorPackageResult.of(contentPackageRepository.save(pkg));
    }
}
