package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RecordScheduledPublicationFailureUseCase {
    private final ContentPackageRepository packages;

    public RecordScheduledPublicationFailureUseCase(ContentPackageRepository packages) {
        this.packages = packages;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID packageId, UUID versionId, String errorCode) {
        ContentPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        ContentPackageVersion version = SchedulePackagePublicationUseCase.findVersion(pkg, versionId);
        if (version.getStatus() == PublicationStatus.DRAFT) {
            version.recordScheduledPublicationError(errorCode);
            packages.save(pkg);
        }
    }
}
