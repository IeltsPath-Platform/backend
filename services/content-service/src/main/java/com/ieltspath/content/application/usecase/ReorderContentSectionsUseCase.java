package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReorderContentSectionsUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public ReorderContentSectionsUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    /**
     * Assigns 1..n in one transaction. Sort order is unique per version, so that check is deferred to commit:
     * checking each row as it is written would fail on the first swap.
     */
    public void execute(UUID packageId, UUID versionId, List<UUID> sectionIds) {
        ContentPackage pkg = contentPackageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        pkg.requireVersion(versionId).requireDraft();
        contentPackageRepository.deferSectionOrderCheck();
        pkg.requireVersion(versionId).reorderSections(sectionIds);
        contentPackageRepository.save(pkg);
    }
}
