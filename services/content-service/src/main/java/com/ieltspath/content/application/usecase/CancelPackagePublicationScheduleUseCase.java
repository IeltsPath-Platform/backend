package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CancelPackagePublicationScheduleUseCase {
    private final ContentPackageRepository packages;

    public CancelPackagePublicationScheduleUseCase(ContentPackageRepository packages) {
        this.packages = packages;
    }

    public void execute(UUID packageId, UUID versionId) {
        ContentPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        ContentPackageVersion version = SchedulePackagePublicationUseCase.findVersion(pkg, versionId);
        if (version.getStatus() != PublicationStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT versions can have a publication schedule");
        }
        version.cancelScheduledPublication();
        packages.save(pkg);
    }
}
