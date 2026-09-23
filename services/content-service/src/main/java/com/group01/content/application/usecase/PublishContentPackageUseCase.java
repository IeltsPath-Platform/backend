package com.group01.content.application.usecase;

import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.exception.InvalidContentStateException;
import com.group01.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishContentPackageUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public PublishContentPackageUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public ContentPackageResult execute(PublishContentPackageCommand command) {
        ContentPackage pkg = contentPackageRepository.findById(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));

        ContentPackageVersion targetVersion = null;
        for (ContentPackageVersion v : pkg.getVersions()) {
            if (v.getId().equals(command.versionId())) {
                targetVersion = v;
                break;
            }
        }

        if (targetVersion == null) {
            throw new InvalidContentStateException("Version " + command.versionId() + " does not belong to package " + command.packageId());
        }

        targetVersion.publish(command.publishedBy());
        pkg.publishVersion(targetVersion.getId());

        ContentPackage saved = contentPackageRepository.save(pkg);

        return new ContentPackageResult(
                saved.getId(),
                saved.getCode(),
                saved.getTitle(),
                saved.getPackageType(),
                saved.getAccessLevel(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}

