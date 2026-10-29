package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** The author's "delete", for a draft or a published package: it is kept, as archived, so that nothing that refers to it breaks. */
@Service
@Transactional
public class ArchiveContentPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public ArchiveContentPackageUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public void execute(UUID packageId) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.archive();
        contentPackageRepository.save(pkg);
    }
}
