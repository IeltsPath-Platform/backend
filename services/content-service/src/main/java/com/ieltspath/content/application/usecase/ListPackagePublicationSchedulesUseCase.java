package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.ScheduledPackagePublicationResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListPackagePublicationSchedulesUseCase {
    private final ContentPackageRepository packages;

    public ListPackagePublicationSchedulesUseCase(ContentPackageRepository packages) {
        this.packages = packages;
    }

    public List<ScheduledPackagePublicationResult> execute(UUID packageId) {
        ContentPackage pkg = packages.findById(packageId)
                .orElseThrow(() -> new ContentPackageNotFoundException(packageId));
        return pkg.getVersions().stream().filter(v -> v.getPublishAt() != null || v.getLastPublishError() != null)
                .map(v -> new ScheduledPackagePublicationResult(packageId, v.getId(), v.getPublishAt(),
                        v.getLastPublishError())).toList();
    }
}
