package com.group01.content.application.usecase;

import com.group01.content.application.command.AddPackageVersionCommand;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.entity.ContentPackageVersion;
import com.group01.content.domain.exception.ContentPackageNotFoundException;
import com.group01.content.domain.repository.ContentPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddPackageVersionUseCase {

    private final ContentPackageRepository contentPackageRepository;

    public AddPackageVersionUseCase(ContentPackageRepository contentPackageRepository) {
        this.contentPackageRepository = contentPackageRepository;
    }

    public ContentPackageResult execute(AddPackageVersionCommand command) {
        ContentPackage pkg = contentPackageRepository.findById(command.packageId())
                .orElseThrow(() -> new ContentPackageNotFoundException(command.packageId()));

        ContentPackageVersion version = ContentPackageVersion.create(
                command.packageId(),
                command.versionNumber(),
                command.rulesJson()
        );

        pkg.addVersion(version);
        ContentPackage saved = contentPackageRepository.save(pkg);

        return new ContentPackageResult(
                saved.getId(),
                saved.getCode(),
                saved.getTitle(),
                saved.getPackageType(),
                saved.getRequiredFeatureKey(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
