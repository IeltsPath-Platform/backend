package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.ScheduledPackagePublicationResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class SchedulePackagePublicationUseCase {
    private final ContentPackageRepository packages;
    private final PublishContentPackageUseCase publish;

    public SchedulePackagePublicationUseCase(ContentPackageRepository packages, PublishContentPackageUseCase publish) {
        this.packages = packages;
        this.publish = publish;
    }

    public ScheduledPackagePublicationResult execute(UUID packageId, UUID versionId, Instant publishAt) {
        if (publishAt == null || !publishAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("publishAt must be in the future");
        }
        ContentPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        ContentPackageVersion version = findVersion(pkg, versionId);
        if (version.getStatus() != PublicationStatus.DRAFT) {
            throw new InvalidContentStateException("Only DRAFT versions can be scheduled");
        }
        publish.validateForPublish(packageId, versionId);
        version.schedulePublication(publishAt);
        packages.save(pkg);
        return new ScheduledPackagePublicationResult(packageId, versionId, version.getPublishAt(),
                version.getLastPublishError());
    }

    static ContentPackageVersion findVersion(ContentPackage pkg, UUID versionId) {
        return pkg.getVersions().stream().filter(v -> v.getId().equals(versionId)).findFirst()
                .orElseThrow(() -> new InvalidContentStateException(
                        "Version " + versionId + " does not belong to package " + pkg.getId()));
    }
}
