package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.AddPackageVersionCommand;
import com.ieltspath.content.application.result.ContentPackageResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
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

        return ContentPackageResult.of(saved);
    }
}
