package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.PlacementPoolLastPublishedException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Hides a practice test or a placement test from learners (ARCHIVED) and brings it back. Attempts already started keep
 * their snapshot, so they can still be finished. The last published test of a placement pool cannot be taken down.
 */
@Service
@Transactional
public class ChangePackageVisibilityUseCase {

    private final ContentPackageRepository packages;

    public ChangePackageVisibilityUseCase(ContentPackageRepository packages) {
        this.packages = packages;
    }

    public AuthorPackageResult archive(UUID packageId) {
        ContentPackage pkg = lock(packageId);
        if (pkg.getStatus() == PublicationStatus.ARCHIVED) {
            return AuthorPackageResult.of(pkg);
        }
        if (pkg.getPackageType() == PackageType.PLACEMENT_TEST && pkg.getStatus() == PublicationStatus.PUBLISHED
                && pkg.getPlacementPool() != null
                && packages.countPublishedPlacementInPool(pkg.getPlacementPool(), pkg.getId()) == 0) {
            throw new PlacementPoolLastPublishedException();
        }
        pkg.archive();
        return AuthorPackageResult.of(packages.save(pkg));
    }

    public AuthorPackageResult restore(UUID packageId) {
        ContentPackage pkg = lock(packageId);
        if (pkg.getStatus() != PublicationStatus.ARCHIVED) {
            return AuthorPackageResult.of(pkg);
        }
        pkg.restore();
        return AuthorPackageResult.of(packages.save(pkg));
    }

    private ContentPackage lock(UUID packageId) {
        return packages.findByIdForUpdate(packageId).orElseThrow(() -> new ContentPackageNotFoundException(packageId));
    }
}
