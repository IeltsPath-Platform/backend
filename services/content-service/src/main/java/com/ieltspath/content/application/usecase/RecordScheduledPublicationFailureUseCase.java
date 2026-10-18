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
    /** The only code of a failure worth retrying: the cause is unknown and may be temporary. */
    public static final String UNKNOWN_FAILURE = "PUBLISH_FAILED";

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
            if (UNKNOWN_FAILURE.equals(errorCode)) {
                version.recordScheduledPublicationError(errorCode);
            } else {
                version.rejectScheduledPublication(errorCode);
            }
            packages.save(pkg);
        }
    }
}
